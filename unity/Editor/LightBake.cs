using System.Collections.Generic;
using UnityEditor;
using UnityEngine;

namespace Bagisik.EditorTools
{
    /// <summary>
    /// Sahneyi pişmiş ışığa (baked lighting) geçirir ve pişirmeyi başlatır.
    ///
    /// Neden: gerçek zamanlı ışık sert, düz ve mobilde pahalı. Pişmiş ışık
    /// yumuşak gölge, ışığın yüzeyden yüzeye sıçraması ve gerçek ortam
    /// kapanması (AO) verir — sahneyi "render" olmaktan çıkarıp "fotoğraf"a
    /// yaklaştıran şey büyük ölçüde bu.
    ///
    /// Işıklar Mixed + Shadowmask olur: dolaylı ışık pişer, doğrudan ışık
    /// gerçek zamanlı kalır (böylece lamba titreyebilir, karakter gölge alır).
    /// Hareketli karakter light probe'lardan aydınlanır.
    ///
    /// Önce 2, 3 ve 5 çalıştırılmış olmalı.
    /// </summary>
    public static class LightBake
    {
        private const string SettingsPath = "Assets/Settings/BagisikLighting.lighting";

        [MenuItem("Bagisik/6 - Isigi Pisir", false, 60)]
        public static void Bake()
        {
            var demo = GameObject.Find("DEMO");
            if (demo == null)
            {
                EditorUtility.DisplayDialog("DEMO sahnesi yok",
                    "Önce 'Bagisik > 2 - Demo Sahnesini Kur' çalıştır.", "Tamam");
                return;
            }

            int staticCount = MarkGeometryStatic(demo.transform);
            ConfigureLights();
            ConfigureEmissive();
            int probes = BuildLightProbes(demo.transform);
            BuildReflectionProbe(demo.transform);
            var settings = ConfigureLightingSettings();

            AssetDatabase.SaveAssets();

            Debug.Log(
                "[Bağışık] Işık pişirmeye hazır.\n" +
                $"• Static geometri: {staticCount} nesne\n" +
                $"• Light probe: {probes} adet (hareketli karakter bunlardan aydınlanır)\n" +
                "• Işıklar Mixed + Shadowmask, emissive ampul GI'ye katkı veriyor\n" +
                $"• Ayarlar: {SettingsPath}\n\n" +
                "Pişirme başlıyor — sahnenin boyutuna ve bilgisayarına göre " +
                "birkaç dakika sürebilir. İlerlemeyi sağ altta görürsün.", settings);

            Lightmapping.BakeAsync();
        }

        [MenuItem("Bagisik/6b - Pismis Isigi Temizle", false, 61)]
        public static void Clear()
        {
            Lightmapping.Clear();
            Lightmapping.ClearLightingDataAsset();
            Debug.Log("[Bağışık] Pişmiş ışık temizlendi. Sahne gerçek zamanlıya döndü.");
        }

        // ---------- Hazırlık ----------

        /// <summary>
        /// Sadece hareketsiz geometri GI'ye katkı verir. Oyuncu ve UI hariç
        /// tutulur — hareketli nesne lightmap'e pişerse sahnede hayaleti kalır.
        /// </summary>
        private static int MarkGeometryStatic(Transform demo)
        {
            var skip = new HashSet<Transform>();
            foreach (string name in new[] { "Player", "UI", "Atmosfer" })
            {
                var t = demo.Find(name);
                if (t != null)
                    foreach (var child in t.GetComponentsInChildren<Transform>(true))
                        skip.Add(child);
            }

            int count = 0;
            foreach (var renderer in demo.GetComponentsInChildren<MeshRenderer>(true))
            {
                if (skip.Contains(renderer.transform)) continue;

                GameObjectUtility.SetStaticEditorFlags(renderer.gameObject,
                    StaticEditorFlags.ContributeGI |
                    StaticEditorFlags.BatchingStatic |
                    StaticEditorFlags.OccluderStatic |
                    StaticEditorFlags.OccludeeStatic |
                    StaticEditorFlags.ReflectionProbeStatic);

                renderer.receiveGI = ReceiveGI.Lightmaps;
                // Küçük dekor parçalarına tam lightmap çözünürlüğü israf;
                // büyük yüzeyler (zemin, duvar, kanopi) detayı hak ediyor.
                renderer.scaleInLightmap = EstimateArea(renderer) > 6f ? 1f : 0.4f;
                count++;
            }

            return count;
        }

        private static float EstimateArea(Renderer renderer)
        {
            Vector3 s = renderer.bounds.size;
            return Mathf.Max(s.x * s.z, s.x * s.y, s.z * s.y);
        }

        private static void ConfigureLights()
        {
            foreach (var light in Object.FindObjectsByType<Light>(FindObjectsSortMode.None))
            {
                // Mixed: dolaylı ışık ve gölge maskesi pişer, doğrudan ışık
                // gerçek zamanlı kalır — hareketli karakter doğru gölge alır.
                light.lightmapBakeType = LightmapBakeType.Mixed;
                light.shadows = LightShadows.Soft;

                // Pişmiş dolaylı ışığı biraz güçlendir: sıcak havuzun duvarlara
                // ve tavana sıçraması, sahneye derinlik veren şey.
                light.bounceIntensity = light.type == LightType.Point ? 2.2f : 1.2f;
            }
        }

        /// <summary>Ampul GI'ye katkı versin — sodyum sıcaklığı çevreye yayılsın.</summary>
        private static void ConfigureEmissive()
        {
            var bulb = AssetDatabase.LoadAssetAtPath<Material>("Assets/Materials/Bagisik_Ampul.mat");
            if (bulb == null) return;

            bulb.globalIlluminationFlags = MaterialGlobalIlluminationFlags.BakedEmissive;
            EditorUtility.SetDirty(bulb);
        }

        // ---------- Probe'lar ----------

        /// <summary>
        /// Hareketli nesneler (karakter) lightmap okumaz; ışığı probe'lardan alır.
        /// Probe yoksa karakter sahneden kopuk, düz aydınlanmış görünür.
        /// </summary>
        private static int BuildLightProbes(Transform parent)
        {
            const string name = "LightProbes";
            var old = parent.Find(name);
            if (old != null) Object.DestroyImmediate(old.gameObject);

            var go = new GameObject(name);
            go.transform.SetParent(parent, false);
            var group = go.AddComponent<LightProbeGroup>();

            var positions = new List<Vector3>();
            // Işık havuzunun kenarında yoğunlaş: gradyanın en çok değiştiği yer orası.
            float[] heights = { 0.35f, 1.4f, 2.8f };
            for (float x = -9f; x <= 9f; x += 3f)
            {
                for (float z = -8f; z <= 8f; z += 3f)
                {
                    foreach (float y in heights)
                        positions.Add(new Vector3(x, y, z));
                }
            }

            group.probePositions = positions.ToArray();
            return positions.Count;
        }

        private static void BuildReflectionProbe(Transform parent)
        {
            const string name = "ReflectionProbe";
            var old = parent.Find(name);
            if (old != null) Object.DestroyImmediate(old.gameObject);

            var go = new GameObject(name);
            go.transform.SetParent(parent, false);
            go.transform.localPosition = new Vector3(0f, 2.5f, 0f);

            var probe = go.AddComponent<ReflectionProbe>();
            probe.mode = UnityEngine.Rendering.ReflectionProbeMode.Baked;
            probe.size = new Vector3(24f, 10f, 24f);
            probe.resolution = 128;      // mobil için yeterli
            probe.hdr = true;
            probe.shadowDistance = 20f;
        }

        // ---------- Pişirme ayarları ----------

        private static LightingSettings ConfigureLightingSettings()
        {
            EnsureFolder("Assets/Settings");

            var settings = AssetDatabase.LoadAssetAtPath<LightingSettings>(SettingsPath);
            if (settings == null)
            {
                settings = new LightingSettings { name = "BagisikLighting" };
                AssetDatabase.CreateAsset(settings, SettingsPath);
            }

            settings.bakedGI = true;
            settings.realtimeGI = false;                      // mobilde kapalı
            settings.mixedBakeMode = MixedLightingMode.Shadowmask;
            settings.lightmapper = LightingSettings.Lightmapper.ProgressiveGPU;
            settings.autoGenerate = false;                    // elle tetiklenir

            // Kalite/süre dengesi: demo sahnesi küçük, yüksek örnek sayısı
            // birkaç dakikaya mal olur ama gürültüsüz sonuç verir.
            settings.directSampleCount = 32;
            settings.indirectSampleCount = 512;
            settings.environmentSampleCount = 256;
            settings.lightmapMaxSize = 2048;
            settings.lightmapResolution = 20f;                // texel/birim
            settings.lightmapPadding = 4;
            settings.compressLightmaps = true;
            settings.ao = true;
            settings.aoMaxDistance = 1.2f;
            settings.aoExponentDirect = 0f;
            settings.aoExponentIndirect = 1.1f;
            settings.filteringMode = LightingSettings.FilterMode.Auto;

            // Yarı-gerçekçi görünüm için şart: normal map'ler pişmiş ışığa
            // tepki versin diye yön bilgisi de pişer.
            settings.directionalityMode = LightmapsMode.CombinedDirectional;

            Lightmapping.lightingSettings = settings;
            EditorUtility.SetDirty(settings);
            return settings;
        }

        private static void EnsureFolder(string path)
        {
            if (!AssetDatabase.IsValidFolder(path))
                AssetDatabase.CreateFolder("Assets", "Settings");
        }
    }
}
