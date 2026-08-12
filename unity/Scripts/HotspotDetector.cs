using System.Collections.Generic;
using UnityEngine;

namespace Bagisik
{
    /// <summary>
    /// Oyuncuya en yakın etkileşim noktasını bulur ve onu "aktif" yapar.
    /// Aynı anda yalnızca BİR hotspot aktiftir — ekran ikon çöplüğüne dönmesin.
    ///
    /// Kurulum:
    ///   1. Oyuncu nesnesine ekle.
    ///   2. Sahnedeki hotspot'lar otomatik bulunur (Register çağırmaya gerek yok).
    ///   3. Arayüz için Active değişimini OnActiveChanged ile dinle.
    /// </summary>
    public class HotspotDetector : MonoBehaviour
    {
        [Tooltip("Aktif hotspot değişince tetiklenir. Yoksa null gelir — UI'ı gizle.")]
        public System.Action<HotspotTarget> OnActiveChanged;

        [Tooltip("Saniyede kaç kez taranacak. Her kare taramak gereksiz — mobilde pil yer.")]
        [SerializeField] private float scansPerSecond = 10f;

        private static readonly List<HotspotTarget> all = new List<HotspotTarget>();
        private HotspotTarget active;
        private float nextScanTime;

        /// <summary>Şu an etkileşilebilir olan hotspot (yoksa null).</summary>
        public HotspotTarget Active => active;

        internal static void Register(HotspotTarget h)
        {
            if (!all.Contains(h)) all.Add(h);
        }

        internal static void Unregister(HotspotTarget h)
        {
            all.Remove(h);
        }

        private void Update()
        {
            if (Time.time < nextScanTime) return;
            nextScanTime = Time.time + (scansPerSecond > 0f ? 1f / scansPerSecond : 0.1f);

            Scan();
        }

        private void Scan()
        {
            HotspotTarget nearest = null;
            float nearestDistance = float.MaxValue;
            Vector3 position = transform.position;

            for (int i = all.Count - 1; i >= 0; i--)
            {
                HotspotTarget h = all[i];

                // Sahne değişiminde yok olmuş referansları temizle.
                if (h == null) { all.RemoveAt(i); continue; }

                if (!h.IsAvailable || !h.IsInRange(position)) continue;

                float d = Vector3.Distance(position, h.PromptPosition);
                if (d < nearestDistance)
                {
                    nearestDistance = d;
                    nearest = h;
                }
            }

            if (nearest == active) return;

            if (active != null) active.SetInRange(false);
            active = nearest;
            if (active != null) active.SetInRange(true);

            OnActiveChanged?.Invoke(active);
        }

        /// <summary>Ekrandaki etkileşim butonuna basıldığında çağır.</summary>
        public void InteractWithActive()
        {
            if (active == null) return;

            active.Interact();

            if (!active.IsAvailable)
            {
                active.SetInRange(false);
                active = null;
                OnActiveChanged?.Invoke(null);
            }
        }
    }
}
