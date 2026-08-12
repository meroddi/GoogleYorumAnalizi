using UnityEngine;

namespace Bagisik
{
    /// <summary>
    /// Sinematik takip kamerası — Cinemachine kullanmadan hafif bir alternatif.
    /// Hedefi yumuşakça takip eder; omuz üstü değil, biraz geriden ve yukarıdan
    /// bakan yarı-sabit bir açı (Detroit / TWD hissi).
    ///
    /// Kurulum:
    ///   1. Main Camera'ya ekle.
    ///   2. Target alanına oyuncuyu sürükle.
    ///   3. Offset ile açıyı ayarla; Play modunda canlı deneyebilirsin.
    /// </summary>
    public class CameraRig : MonoBehaviour
    {
        [Header("Hedef")]
        [SerializeField] private Transform target;

        [Tooltip("Ayaklara değil, gövde/omuz hizasına bak.")]
        [SerializeField] private float lookHeight = 1.35f;

        [Header("Konum")]
        [Tooltip("Hedefe göre kamera ofseti (dünya uzayında). Y = yükseklik, Z = geri mesafe.")]
        [SerializeField] private Vector3 offset = new Vector3(0f, 3.2f, -4.5f);

        [Tooltip("Takip yumuşaklığı (saniye). Büyük değer = daha tembel, sinematik kamera.")]
        [SerializeField] private float followSmoothTime = 0.25f;

        [Header("Bakış")]
        [SerializeField] private float rotationSmoothSpeed = 6f;

        [Header("Çarpışma")]
        [Tooltip("Kamera ile hedef arasına duvar girerse kamerayı öne çeker.")]
        [SerializeField] private bool avoidWalls = true;
        [SerializeField] private LayerMask wallLayers = ~0;
        [SerializeField] private float wallPadding = 0.3f;

        private Vector3 followVelocity;

        private void LateUpdate()
        {
            if (target == null) return;

            Vector3 focusPoint = target.position + Vector3.up * lookHeight;
            Vector3 desiredPosition = target.position + offset;

            if (avoidWalls)
                desiredPosition = PullInFrontOfWalls(focusPoint, desiredPosition);

            transform.position = Vector3.SmoothDamp(
                transform.position, desiredPosition, ref followVelocity, followSmoothTime);

            Quaternion desiredRotation = Quaternion.LookRotation(focusPoint - transform.position);
            transform.rotation = Quaternion.Slerp(
                transform.rotation, desiredRotation, rotationSmoothSpeed * Time.deltaTime);
        }

        /// <summary>Odak noktasından kameraya ışın atar; duvara çarparsa kamerayı beriye alır.</summary>
        private Vector3 PullInFrontOfWalls(Vector3 focusPoint, Vector3 desiredPosition)
        {
            Vector3 toCamera = desiredPosition - focusPoint;
            float distance = toCamera.magnitude;
            if (distance < 0.01f) return desiredPosition;

            if (Physics.Raycast(focusPoint, toCamera.normalized, out RaycastHit hit,
                                distance, wallLayers, QueryTriggerInteraction.Ignore))
            {
                return focusPoint + toCamera.normalized * Mathf.Max(0.5f, hit.distance - wallPadding);
            }

            return desiredPosition;
        }

        /// <summary>Sahne geçişlerinde kamerayı anında yerine oturtmak için.</summary>
        public void SnapToTarget()
        {
            if (target == null) return;

            transform.position = target.position + offset;
            transform.rotation = Quaternion.LookRotation(
                (target.position + Vector3.up * lookHeight) - transform.position);
            followVelocity = Vector3.zero;
        }
    }
}
