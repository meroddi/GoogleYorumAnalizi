using UnityEditor;
using UnityEditor.Build;
using UnityEngine;
using UnityEngine.Rendering;
using UnityEngine.Rendering.Universal;

namespace Bagisik.EditorTools
{
    /// <summary>
    /// Android + URP mobil ayarlarını tek tıkla uygular.
    /// Değerler docs/SANAT-YONU.md'deki baseline'dan gelir.
    ///
    /// Not: bu ayarlar başlangıç noktasıdır, son söz değil. Sanat yönü belgesi
    /// birkaç maddenin GERÇEK CİHAZDA ölçülmesi gerektiğini söylüyor —
    /// özellikle MSAA+HDR birlikte, kare temposu (frame pacing) ve on-tile
    /// post-processing. Konsol çıktısındaki uyarıları oku.
    /// </summary>
    public static class ProjectSetup
    {
        [MenuItem("Bagisik/1 - Proje Ayarlarini Uygula", false, 10)]
        public static void Apply()
        {
            ApplyAndroidSettings();
            int urpCount = ApplyUrpSettings();

            AssetDatabase.SaveAssets();

            Debug.Log(
                "[Bağışık] Proje ayarları uygulandı.\n" +
                $"• Android: IL2CPP · ARM64 · Vulkan+GLES3 · minSdk 26 · yatay\n" +
                $"• URP asset güncellendi: {urpCount} adet\n" +
                "• Kare hızı: sahnedeki bir nesneye FrameRateController ekle.\n\n" +
                "CİHAZDA DOĞRULANACAKLAR (sanat yönü belgesi §Düzeltmeler):\n" +
                "  1. HDR + MSAA 2x birlikte 15 dk — bazı Adreno sürücülerinde çökme raporu var.\n" +
                "  2. Kare temposu: yavaş bir kamera kaydırmasında titreme (judder) var mı?\n" +
                "  3. Post-process mimarisi (on-tile vs klasik) — ölçmeden karar verme.");
        }

        private static void ApplyAndroidSettings()
        {
            var android = NamedBuildTarget.Android;

            // Play Store 64-bit zorunlu kılıyor; IL2CPP da onun ön koşulu.
            PlayerSettings.SetScriptingBackend(android, ScriptingImplementation.IL2CPP);
            PlayerSettings.Android.targetArchitectures = AndroidArchitecture.ARM64;

            // Vulkan önde, GLES3 yedekte. GLES3'ü SİLME: Google'ın VkQuality
            // eklentisi bozuk sürücülü cihazları GLES'e düşürebilmek için
            // ikisinin de açık olmasını istiyor.
            PlayerSettings.SetUseDefaultGraphicsAPIs(BuildTarget.Android, false);
            PlayerSettings.SetGraphicsAPIs(BuildTarget.Android, new[]
            {
                GraphicsDeviceType.Vulkan,
                GraphicsDeviceType.OpenGLES3,
            });

            PlayerSettings.Android.minSdkVersion = AndroidSdkVersions.AndroidApiLevel26;

            // Sinematik bir oyun; ekran döndürme yok.
            PlayerSettings.defaultInterfaceOrientation = UIOrientation.LandscapeLeft;
            PlayerSettings.allowedAutorotateToPortrait = false;
            PlayerSettings.allowedAutorotateToPortraitUpsideDown = false;

            // Kare temposunu Unity'nin resmî yoluna bırak. vSync kapalı +
            // targetFrameRate kombinasyonu Android'de judder üretiyor.
            PlayerSettings.Android.optimizedFramePacing = true;

            PlayerSettings.colorSpace = ColorSpace.Linear;
        }

        /// <summary>Projedeki tüm URP asset'lerini mobil profiline çeker.</summary>
        private static int ApplyUrpSettings()
        {
            int touched = 0;

            foreach (string guid in AssetDatabase.FindAssets("t:UniversalRenderPipelineAsset"))
            {
                string path = AssetDatabase.GUIDToAssetPath(guid);
                var asset = AssetDatabase.LoadAssetAtPath<UniversalRenderPipelineAsset>(path);
                if (asset == null) continue;

                // Gölge: kısa mesafe + az cascade. Mobilde en pahalı kalemlerden.
                asset.shadowDistance = 25f;
                asset.shadowCascadeCount = 2;

                // MSAA 2x — tile GPU'da görece ucuz ve kenarları belirgin toparlar.
                asset.msaaSampleCount = 2;

                // HDR: grade ve tonemapping için gerekli.
                asset.supportsHDR = true;

                asset.renderScale = 1f;

                // Bu ikisi mobilde tam ekran resolve maliyeti getirir; kapalı kalsın.
                asset.supportsCameraOpaqueTexture = false;
                asset.supportsCameraDepthTexture = true;

                EditorUtility.SetDirty(asset);
                touched++;
            }

            return touched;
        }
    }
}
