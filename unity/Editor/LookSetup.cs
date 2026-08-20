using System.IO;
using UnityEditor;
using UnityEngine;
using UnityEngine.Rendering;
using UnityEngine.Rendering.Universal;

namespace Bagisik.EditorTools
{
    /// <summary>
    /// Sanat yönü anayasasındaki görüntüyü sahneye uygular:
    /// tek sıcak key ışığı, soğuk gölgeler, sis, palet materyalleri ve
    /// filmik bir post-process yığını.
    ///
    /// Değerler docs/SANAT-YONU.md'den gelir. Bu bir başlangıç noktasıdır —
    /// nihai görüntü sahne sahne elle ayarlanır.
    /// </summary>
    public static class LookSetup
    {
        // --- Palet (docs/SANAT-YONU.md §2) ---
        private static readonly Color ShadowDeep  = Hex("#1B242C");
        private static readonly Color ShadowMid   = Hex("#2A3742");
        private static readonly Color ShadowLight = Hex("#38454E");
        private static readonly Color NeutralWarm = Hex("#4A4A44");
        private static readonly Color KeyDusk     = Hex("#E8A24C"); // gündüz/alacakaranlık sahneleri için
        private static readonly Color Sodium      = Hex("#FFA24A");
        private static readonly Color Moonlight   = Hex("#8FA8C4");
        private static readonly Color FogColor    = Hex("#1C2228");

        private const string SettingsDir = "Assets/Settings";
        private const string ProfilePath = SettingsDir + "/BagisikLook.asset";
        private const string MaterialDir = "Assets/Materials";

        [MenuItem("Bagisik/3 - Atmosferi Uygula", false, 30)]
        public static void Apply()
        {
            var demo = GameObject.Find("DEMO");
            if (demo == null)
            {
                EditorUtility.DisplayDialog(
                    "DEMO sahnesi yok",
                    "Önce 'Bagisik > 2 - Demo Sahnesini Kur' komutunu çalıştır.",
                    "Tamam");
                return;
            }

            ApplyEnvironment();
            ApplyKeyLight();
            AddPracticalLamp(demo.transform);
            ApplyMaterials(demo.transform);
            var profile = BuildVolumeProfile();
            AttachVolume(demo.transform, profile);
            EnableCameraPostProcessing();

            AssetDatabase.SaveAssets();
            EditorApplication.QueuePlayerLoopUpdate();
            SceneView.RepaintAll();

            Debug.Log(
                "[Bağışık] Atmosfer uygulandı.\n" +
                "• Alacakaranlık key ışığı + soğuk ortam + sis\n" +
                "• Sodyum lamba (görünür kaynağıyla) — 'her key ışığın bir kaynağı var' kuralı\n" +
                "• Palet materyalleri zemin/duvar/engellere atandı\n" +
                $"• Post-process profili: {ProfilePath}\n\n" +
                "AYARLAMAK İÇİN: DEMO > Atmosfer nesnesindeki Volume profilini seç, " +
                "Inspector'da canlı oyna. Işık için DEMO > SodyumLamba ve Directional Light.");
        }

        // ---------- Ortam ----------

        private static void ApplyEnvironment()
        {
            // Gökyüzü yerine düz koyu bir zemin: sis zaten mesafeyi yutuyor,
            // ve skybox'ın parlak gradyanı paleti bozuyor.
            RenderSettings.skybox = null;
            // Gölgeler siyaha kırpılmayacak: karanlığın içinde hâlâ okunacak
            // detay kalmalı (anayasa §1, kural 2). Ortam ışığı bunun sigortası.
            RenderSettings.ambientMode = AmbientMode.Trilight;
            RenderSettings.ambientSkyColor = ShadowLight * 1.15f;
            RenderSettings.ambientEquatorColor = ShadowMid * 1.0f;
            RenderSettings.ambientGroundColor = ShadowDeep * 0.85f;

            // Sis her sahnede açık; rengi sahnenin baskın ışığından örneklenir.
            RenderSettings.fog = true;
            RenderSettings.fogMode = FogMode.Linear;
            RenderSettings.fogColor = FogColor;
            RenderSettings.fogStartDistance = 8f;
            RenderSettings.fogEndDistance = 45f;

            foreach (var cam in Object.FindObjectsByType<Camera>(FindObjectsSortMode.None))
            {
                cam.clearFlags = CameraClearFlags.SolidColor;
                cam.backgroundColor = FogColor;
            }
        }

        private static void ApplyKeyLight()
        {
            foreach (var light in Object.FindObjectsByType<Light>(FindObjectsSortMode.None))
            {
                if (light.type != LightType.Directional) continue;

                // AY IŞIĞI — key değil, dolgu. Soğuk ve zayıf.
                // Sahnenin gerçek key'i sodyum lambası; sıcak ada onun etrafında
                // kurulsun diye bu ışık bilinçli olarak silik tutuluyor.
                light.color = Moonlight;
                light.intensity = 0.5f;
                light.transform.rotation = Quaternion.Euler(38f, 150f, 0f);
                light.shadows = LightShadows.Soft;
                light.shadowStrength = 0.55f;
            }
        }

        private static void AddPracticalLamp(Transform parent)
        {
            const string name = "SodyumLamba";
            var old = parent.Find(name);
            if (old != null) Object.DestroyImmediate(old.gameObject);

            var root = new GameObject(name);
            root.transform.SetParent(parent, false);
            root.transform.localPosition = new Vector3(-5f, 0f, 4f);

            // Direk — kural: her key ışığın görünen bir kaynağı olacak.
            var pole = GameObject.CreatePrimitive(PrimitiveType.Cylinder);
            pole.name = "Direk";
            pole.transform.SetParent(root.transform, false);
            pole.transform.localPosition = new Vector3(0f, 2.2f, 0f);
            pole.transform.localScale = new Vector3(0.12f, 2.2f, 0.12f);
            pole.GetComponent<MeshRenderer>().sharedMaterial =
                GetOrCreateMaterial("Bagisik_Metal", ShadowDeep, 0.55f);

            var head = GameObject.CreatePrimitive(PrimitiveType.Sphere);
            head.name = "Ampul";
            head.transform.SetParent(root.transform, false);
            head.transform.localPosition = new Vector3(0f, 4.5f, 0f);
            head.transform.localScale = Vector3.one * 0.35f;
            head.GetComponent<MeshRenderer>().sharedMaterial =
                GetOrCreateEmissive("Bagisik_Ampul", Sodium, 0.85f);

            var lightGo = new GameObject("Isik");
            lightGo.transform.SetParent(root.transform, false);
            lightGo.transform.localPosition = new Vector3(0f, 4.4f, 0f);

            var light = lightGo.AddComponent<Light>();
            light.type = LightType.Point;
            light.color = Sodium;
            // Menzil bilinçli olarak kısa: ışık tüm sahneyi yıkarsa "sıcak ada"
            // diye bir şey kalmaz, sadece turuncu bir oda olur. Havuzun kenarı
            // görünmeli ki karanlık bir anlam taşısın.
            light.intensity = 14f;
            light.range = 11f;
            light.shadows = LightShadows.Soft;
        }

        // ---------- Materyaller ----------

        private static void ApplyMaterials(Transform demo)
        {
            var ground = GetOrCreateMaterial("Bagisik_Zemin", ShadowMid, 0.85f);
            var wall = GetOrCreateMaterial("Bagisik_Duvar", ShadowLight, 0.9f);
            var prop = GetOrCreateMaterial("Bagisik_Engel", NeutralWarm, 0.8f);

            AssignTo(demo.Find("Zemin"), ground);
            AssignTo(demo.Find("Duvarlar"), wall);
            AssignTo(demo.Find("Engeller"), prop);

            var player = demo.Find("Player");
            if (player != null)
                AssignTo(player, GetOrCreateMaterial("Bagisik_Karakter", Hex("#5E5850"), 0.7f));
        }

        private static void AssignTo(Transform target, Material material)
        {
            if (target == null) return;

            foreach (var renderer in target.GetComponentsInChildren<MeshRenderer>(true))
                renderer.sharedMaterial = material;
        }

        private static Material GetOrCreateMaterial(string name, Color color, float smoothnessInverse)
        {
            EnsureFolder(MaterialDir);
            string path = $"{MaterialDir}/{name}.mat";

            var mat = AssetDatabase.LoadAssetAtPath<Material>(path);
            bool isNew = mat == null;
            if (isNew) mat = new Material(Shader.Find("Universal Render Pipeline/Lit")) { name = name };

            mat.SetColor("_BaseColor", color);
            // Post-apokaliptik yüzeyler mat: parlaklık plastik görünümün baş sebebi.
            mat.SetFloat("_Smoothness", Mathf.Clamp01(1f - smoothnessInverse));
            mat.SetFloat("_Metallic", 0f);

            if (isNew) AssetDatabase.CreateAsset(mat, path);
            else EditorUtility.SetDirty(mat);
            return mat;
        }

        private static Material GetOrCreateEmissive(string name, Color color, float intensity)
        {
            EnsureFolder(MaterialDir);
            string path = $"{MaterialDir}/{name}.mat";

            var mat = AssetDatabase.LoadAssetAtPath<Material>(path);
            bool isNew = mat == null;
            if (isNew) mat = new Material(Shader.Find("Universal Render Pipeline/Lit")) { name = name };

            mat.SetColor("_BaseColor", color);
            mat.EnableKeyword("_EMISSION");
            mat.globalIlluminationFlags = MaterialGlobalIlluminationFlags.RealtimeEmissive;
            mat.SetColor("_EmissionColor", color * intensity);

            if (isNew) AssetDatabase.CreateAsset(mat, path);
            else EditorUtility.SetDirty(mat);
            return mat;
        }

        // ---------- Post-process ----------

        private static VolumeProfile BuildVolumeProfile()
        {
            EnsureFolder(SettingsDir);

            var profile = AssetDatabase.LoadAssetAtPath<VolumeProfile>(ProfilePath);
            if (profile == null)
            {
                profile = ScriptableObject.CreateInstance<VolumeProfile>();
                AssetDatabase.CreateAsset(profile, ProfilePath);
            }

            // Neutral, ACES değil: ACES doygun kırmızıyı turuncuya kaydırır ve
            // Ela'nın kapüşonunu tam da parladığı anda öldürür.
            var tonemap = GetOrAdd<Tonemapping>(profile);
            tonemap.mode.overrideState = true;
            tonemap.mode.value = TonemappingMode.Neutral;

            var grade = GetOrAdd<ColorAdjustments>(profile);
            grade.postExposure.overrideState = true;
            grade.postExposure.value = -0.15f;
            grade.contrast.overrideState = true;
            grade.contrast.value = 12f;
            grade.saturation.overrideState = true;
            grade.saturation.value = -28f;   // ölü dünya; aksan rengi shader'da muaf tutulacak

            var wb = GetOrAdd<WhiteBalance>(profile);
            wb.temperature.overrideState = true;
            wb.temperature.value = -12f;     // gölgeleri soğut
            wb.tint.overrideState = true;
            wb.tint.value = 4f;

            var smh = GetOrAdd<ShadowsMidtonesHighlights>(profile);
            smh.shadows.overrideState = true;
            smh.shadows.value = new Vector4(0.88f, 0.96f, 1.14f, 0.055f); // maviye + hafif kaldır
            smh.highlights.overrideState = true;
            smh.highlights.value = new Vector4(1.1f, 1.02f, 0.9f, 0f);   // ışıklar sıcağa

            var vignette = GetOrAdd<Vignette>(profile);
            vignette.intensity.overrideState = true;
            vignette.intensity.value = 0.26f;
            vignette.smoothness.overrideState = true;
            vignette.smoothness.value = 0.45f;

            var grain = GetOrAdd<FilmGrain>(profile);
            grain.type.overrideState = true;
            grain.type.value = FilmGrainLookup.Medium1;
            grain.intensity.overrideState = true;
            grain.intensity.value = 0.28f;
            grain.response.overrideState = true;
            grain.response.value = 0.75f;

            EditorUtility.SetDirty(profile);
            return profile;
        }

        private static T GetOrAdd<T>(VolumeProfile profile) where T : VolumeComponent
        {
            return profile.TryGet<T>(out var component) ? component : profile.Add<T>(true);
        }

        private static void AttachVolume(Transform parent, VolumeProfile profile)
        {
            const string name = "Atmosfer";
            var existing = parent.Find(name);
            var go = existing != null ? existing.gameObject : new GameObject(name);

            if (existing == null) go.transform.SetParent(parent, false);

            var volume = go.GetComponent<Volume>() ?? go.AddComponent<Volume>();
            volume.isGlobal = true;
            volume.priority = 0f;
            volume.weight = 1f;
            volume.sharedProfile = profile;
        }

        private static void EnableCameraPostProcessing()
        {
            foreach (var cam in Object.FindObjectsByType<Camera>(FindObjectsSortMode.None))
            {
                var data = cam.GetComponent<UniversalAdditionalCameraData>();
                if (data == null) data = cam.gameObject.AddComponent<UniversalAdditionalCameraData>();
                data.renderPostProcessing = true;
                data.antialiasing = AntialiasingMode.FastApproximateAntialiasing;
            }
        }

        // ---------- Yardımcılar ----------

        private static void EnsureFolder(string path)
        {
            if (AssetDatabase.IsValidFolder(path)) return;

            string parent = Path.GetDirectoryName(path).Replace('\\', '/');
            string leaf = Path.GetFileName(path);
            AssetDatabase.CreateFolder(parent, leaf);
        }

        private static Color Hex(string hex)
        {
            return ColorUtility.TryParseHtmlString(hex, out var c) ? c : Color.magenta;
        }
    }
}
