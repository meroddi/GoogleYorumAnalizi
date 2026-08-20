using UnityEngine;

namespace Bagisik
{
    /// <summary>
    /// Kare hızını sabitler ve isteğe bağlı bir FPS sayacı gösterir.
    /// Sahnedeki herhangi bir kalıcı nesneye ekle.
    ///
    /// NEDEN 30: 30 fps kilidi, kare başına GPU bütçesini ~10 ms'den ~20 ms'ye
    /// çıkarır — yani "filmik" ile "ucuz"u ayıran efektleri (gölge, SSAO, grade)
    /// karşılayabilmenin tek yolu. TWD S1 ve Detroit de 30'da çıktı.
    ///
    /// DİKKAT — cihazda doğrula: vSync KAPALI + targetFrameRate kombinasyonu
    /// Android'de judder üretiyor (Unity'nin kendi dokümanı uyarıyor). Burada
    /// Unity'nin önerdiği yol kullanılıyor: Optimized Frame Pacing (Player
    /// Settings'te açık) + targetFrameRate. Yavaş bir kamera kaydırmasıyla
    /// telefonda test et; titreme görürsen alternatifleri dene.
    /// </summary>
    public class FrameRateController : MonoBehaviour
    {
        [SerializeField] private int targetFrameRate = 30;

        [Tooltip("Ekranda kare süresi göster. Yayında kapat.")]
        [SerializeField] private bool showCounter = true;

        private float smoothedDeltaTime;
        private GUIStyle style;

        private void Awake()
        {
            Application.targetFrameRate = targetFrameRate;

            // Ekran uyku moduna girmesin — test sırasında can sıkıcı.
            Screen.sleepTimeout = SleepTimeout.NeverSleep;

            DontDestroyOnLoad(gameObject);
        }

        private void Update()
        {
            smoothedDeltaTime += (Time.unscaledDeltaTime - smoothedDeltaTime) * 0.1f;
        }

        private void OnGUI()
        {
            if (!showCounter) return;

            if (style == null)
            {
                style = new GUIStyle(GUI.skin.label)
                {
                    fontSize = Mathf.RoundToInt(Screen.height * 0.03f),
                    alignment = TextAnchor.UpperLeft,
                };
                style.normal.textColor = Color.white;
            }

            float ms = smoothedDeltaTime * 1000f;
            float fps = smoothedDeltaTime > 0f ? 1f / smoothedDeltaTime : 0f;

            var rect = new Rect(Screen.width * 0.02f, Screen.height * 0.02f,
                                Screen.width * 0.5f, Screen.height * 0.1f);
            GUI.Label(rect, $"{ms:0.0} ms  ({fps:0} fps)", style);
        }
    }
}
