using UnityEngine;
using UnityEngine.Events;

namespace Bagisik
{
    public enum HotspotKind
    {
        /// <summary>Göz ikonu — incele, bir replik ya da açıklama tetikle.</summary>
        Examine,
        /// <summary>El ikonu — al, envantere ekle, kapıyı aç.</summary>
        Take,
        /// <summary>Konuşma — bir karakterle diyalog başlat.</summary>
        Talk
    }

    /// <summary>
    /// Etkileşim noktası — Detroit modeli: oyuncu yaklaşınca ipucu belirir,
    /// dokununca aksiyon tetiklenir.
    ///
    /// Kurulum:
    ///   1. Etkileşilecek nesneye ekle (sandık, raf, kapı, NPC).
    ///   2. Kind seç (göz / el / konuşma) ve Label yaz.
    ///   3. OnInteract olayına ne olacağını bağla.
    ///   4. Görsel ipucu (parlama/ikon) için Prompt Visual'a bir child nesne ver.
    /// </summary>
    public class HotspotTarget : MonoBehaviour
    {
        [Header("Tanım")]
        [SerializeField] private HotspotKind kind = HotspotKind.Examine;

        [Tooltip("Ekranda görünecek ad — ör. 'ERZAK SANDIĞI'.")]
        [SerializeField] private string label = "NESNE";

        [Header("Menzil")]
        [Tooltip("Oyuncu bu mesafeye girince ipucu belirir.")]
        [SerializeField] private float activationRange = 2.5f;

        [Tooltip("Boş bırakılırsa bu nesnenin merkezi kullanılır.")]
        [SerializeField] private Transform promptAnchor;

        [Header("Görsel ipucu")]
        [Tooltip("Yaklaşınca açılacak nesne (parlama, ikon, outline).")]
        [SerializeField] private GameObject promptVisual;

        [Header("Davranış")]
        [Tooltip("Açıksa yalnızca bir kez etkileşilebilir (ör. yerden alınan eşya).")]
        [SerializeField] private bool oneShot = false;

        [Header("Olay")]
        public UnityEvent OnInteract;

        public HotspotKind Kind => kind;
        public string Label => label;
        public bool IsAvailable => !(oneShot && used);
        public Vector3 PromptPosition => promptAnchor != null ? promptAnchor.position : transform.position;

        private bool used;
        private bool inRange;

        private void Awake()
        {
            SetPromptVisible(false);
        }

        private void OnEnable()
        {
            HotspotDetector.Register(this);
        }

        private void OnDisable()
        {
            HotspotDetector.Unregister(this);
            SetPromptVisible(false);
            inRange = false;
        }

        /// <summary>Oyuncu menzilde mi? HotspotDetector her karede çağırır.</summary>
        public bool IsInRange(Vector3 playerPosition)
        {
            return Vector3.Distance(playerPosition, PromptPosition) <= activationRange;
        }

        /// <summary>Menzile girildi/çıkıldı — görsel ipucunu aç/kapat.</summary>
        public void SetInRange(bool value)
        {
            if (inRange == value) return;
            inRange = value;
            SetPromptVisible(value && IsAvailable);
        }

        /// <summary>Oyuncu ipucuna dokundu.</summary>
        public void Interact()
        {
            if (!IsAvailable) return;

            used = true;
            OnInteract?.Invoke();

            if (oneShot)
            {
                SetPromptVisible(false);
                inRange = false;
            }
        }

        private void SetPromptVisible(bool visible)
        {
            if (promptVisual != null) promptVisual.SetActive(visible);
        }

        private void OnDrawGizmosSelected()
        {
            Gizmos.color = new Color(0.88f, 0.64f, 0.35f, 0.65f); // alacakaranlık kehribarı
            Gizmos.DrawWireSphere(PromptPosition, activationRange);
        }
    }
}
