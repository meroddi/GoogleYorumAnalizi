using UnityEngine;
using UnityEngine.InputSystem;

namespace Bagisik
{
    /// <summary>
    /// Sınırlı serbest yürüme — Detroit / TWD modeli.
    /// Ekrandaki joystick girdisini kameraya göre dünya yönüne çevirir ve
    /// CharacterController ile hareket ettirir. Animator'e "Speed" gönderir.
    ///
    /// Kurulum:
    ///   1. Oyuncu nesnesine CharacterController ekle.
    ///   2. Bu script'i ekle, Move Action'a Input Action'ını bağla (Vector2).
    ///   3. Animator varsa alana sürükle (yoksa boş bırak — kapsülle test için).
    /// </summary>
    [RequireComponent(typeof(CharacterController))]
    public class PlayerMover : MonoBehaviour
    {
        [Header("Girdi")]
        [Tooltip("Vector2 tipinde bir Move action. Ekrandaki On-Screen Stick buna bağlanır.")]
        [SerializeField] private InputActionReference moveAction;

        [Header("Hareket")]
        [SerializeField] private float walkSpeed = 2.2f;
        [SerializeField] private float runSpeed = 4.4f;

        [Tooltip("Joystick bu eğimin altındaysa yürür, üstündeyse koşuya geçer (0-1).")]
        [Range(0.1f, 0.95f)]
        [SerializeField] private float runTiltThreshold = 0.7f;

        [Tooltip("Hıza ulaşma yumuşaklığı. Küçük değer = daha keskin/tepkili.")]
        [SerializeField] private float acceleration = 12f;

        [Tooltip("Karakterin gideceği yöne dönme hızı (derece/saniye).")]
        [SerializeField] private float turnSpeed = 720f;

        [Header("Yerçekimi")]
        [SerializeField] private float gravity = -18f;

        [Header("Referanslar")]
        [Tooltip("Boş bırakılırsa Camera.main kullanılır.")]
        [SerializeField] private Transform cameraTransform;

        [Tooltip("İsteğe bağlı. Idle/Walk blend için 'Speed' float parametresi beklenir.")]
        [SerializeField] private Animator animator;

        private CharacterController controller;
        private Vector3 horizontalVelocity;
        private float verticalVelocity;
        private static readonly int SpeedHash = Animator.StringToHash("Speed");

        /// <summary>Normalize edilmiş hız (0 = duruyor, 1 = koşuyor). Animasyon ve UI için.</summary>
        public float NormalizedSpeed { get; private set; }

        private void Awake()
        {
            controller = GetComponent<CharacterController>();

            if (cameraTransform == null && Camera.main != null)
                cameraTransform = Camera.main.transform;
        }

        private void OnEnable()
        {
            if (moveAction != null) moveAction.action.Enable();
        }

        private void OnDisable()
        {
            if (moveAction != null) moveAction.action.Disable();
        }

        private void Update()
        {
            Vector2 input = ReadInput();

            // Joystick eğimi ne kadarsa o kadar hızlı: eşiğe kadar yürüyüş,
            // sonrasında koşuya doğru artar. Tam itiş = koşu.
            float tilt = Mathf.Clamp01(input.magnitude);
            float targetSpeed = tilt <= runTiltThreshold
                ? Mathf.Lerp(0f, walkSpeed, tilt / runTiltThreshold)
                : Mathf.Lerp(walkSpeed, runSpeed, (tilt - runTiltThreshold) / (1f - runTiltThreshold));

            Vector3 desiredDirection = InputToWorldDirection(input);
            Vector3 targetVelocity = desiredDirection * targetSpeed;

            horizontalVelocity = Vector3.MoveTowards(
                horizontalVelocity,
                targetVelocity,
                acceleration * Time.deltaTime);

            ApplyGravity();
            FaceMovementDirection(desiredDirection);

            Vector3 motion = horizontalVelocity + Vector3.up * verticalVelocity;
            controller.Move(motion * Time.deltaTime);

            NormalizedSpeed = runSpeed > 0f ? horizontalVelocity.magnitude / runSpeed : 0f;
            if (animator != null) animator.SetFloat(SpeedHash, NormalizedSpeed, 0.1f, Time.deltaTime);
        }

        private Vector2 ReadInput()
        {
            if (moveAction != null)
                return moveAction.action.ReadValue<Vector2>();

            // Editörde klavyeyle test için yedek — mobilde kullanılmaz.
            var keyboard = Keyboard.current;
            if (keyboard == null) return Vector2.zero;

            float x = (keyboard.dKey.isPressed ? 1f : 0f) - (keyboard.aKey.isPressed ? 1f : 0f);
            float y = (keyboard.wKey.isPressed ? 1f : 0f) - (keyboard.sKey.isPressed ? 1f : 0f);
            return Vector2.ClampMagnitude(new Vector2(x, y), 1f);
        }

        /// <summary>
        /// Girdiyi kameranın baktığı yöne göre dünya yönüne çevirir:
        /// joystick'i yukarı ittiğinde karakter "ekranda yukarı" gider — kamera nereye
        /// bakarsa baksın. Sinematik/dönen kameralarda doğru his için şart.
        /// </summary>
        private Vector3 InputToWorldDirection(Vector2 input)
        {
            if (input.sqrMagnitude < 0.0001f) return Vector3.zero;

            Vector3 forward = Vector3.forward;
            Vector3 right = Vector3.right;

            if (cameraTransform != null)
            {
                forward = Vector3.ProjectOnPlane(cameraTransform.forward, Vector3.up).normalized;
                right = Vector3.ProjectOnPlane(cameraTransform.right, Vector3.up).normalized;
            }

            return (forward * input.y + right * input.x).normalized;
        }

        private void FaceMovementDirection(Vector3 direction)
        {
            if (direction.sqrMagnitude < 0.0001f) return;

            Quaternion target = Quaternion.LookRotation(direction, Vector3.up);
            transform.rotation = Quaternion.RotateTowards(
                transform.rotation, target, turnSpeed * Time.deltaTime);
        }

        private void ApplyGravity()
        {
            if (controller.isGrounded && verticalVelocity < 0f)
                verticalVelocity = -2f; // zemine hafif bastır — eğimlerde zıplamayı önler
            else
                verticalVelocity += gravity * Time.deltaTime;
        }
    }
}
