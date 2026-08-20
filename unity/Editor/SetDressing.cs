using UnityEditor;
using UnityEngine;

namespace Bagisik.EditorTools
{
    /// <summary>
    /// Gri kutu sahnesini terk edilmiş bir benzin istasyonuna çevirir.
    ///
    /// Hiçbir indirme gerektirmez — her şey ilkel şekillerden kurulur.
    /// Sanat anayasasının ilkesi: karanlık ve sis bütçeyi gizler; önemli olan
    /// mekânın bir YER gibi okunması, poligon sayısı değil.
    ///
    /// Önce 2 (sahne) ve 3 (atmosfer) çalıştırılmış olmalı.
    /// </summary>
    public static class SetDressing
    {
        private const string Root = "Mekan";
        private const int Seed = 20260812;   // sabit tohum: her çalıştırmada aynı sonuç

        // Palet (docs/SANAT-YONU.md §2)
        private static readonly Color Asphalt  = Hex("#22282E");
        private static readonly Color Concrete = Hex("#38414A");
        private static readonly Color Metal    = Hex("#2E3640");
        private static readonly Color RustMet  = Hex("#4A413A");
        private static readonly Color Wood     = Hex("#4A4238");
        private static readonly Color Glass    = Hex("#141A20");
        private static readonly Color Paint    = Hex("#5E5850");

        [MenuItem("Bagisik/5 - Mekani Giydir", false, 50)]
        public static void Dress()
        {
            var demo = GameObject.Find("DEMO");
            if (demo == null)
            {
                EditorUtility.DisplayDialog("DEMO sahnesi yok",
                    "Önce 'Bagisik > 2 - Demo Sahnesini Kur' çalıştır.", "Tamam");
                return;
            }

            var old = demo.transform.Find(Root);
            if (old != null) Object.DestroyImmediate(old.gameObject);

            // Kutu engeller artık gereksiz — yerlerini gerçek dekor alıyor.
            var boxes = demo.transform.Find("Engeller");
            if (boxes != null) boxes.gameObject.SetActive(false);

            var root = new GameObject(Root);
            root.transform.SetParent(demo.transform, false);
            Undo.RegisterCreatedObjectUndo(root, "Mekanı giydir");

            Random.InitState(Seed);

            BuildForecourt(root.transform);
            BuildCanopy(root.transform);
            BuildPumps(root.transform);
            BuildShop(root.transform);
            BuildWreck(root.transform);
            BuildProps(root.transform);

            MarkStatic(root);
            AssetDatabase.SaveAssets();

            Debug.Log(
                "[Bağışık] Mekân giydirildi: benzin istasyonu.\n" +
                "• Kanopi, iki pompa, dükkân cephesi, hurda araç, variller, moloz\n" +
                "• Eski gri kutular ('Engeller') gizlendi\n\n" +
                "Sodyum lambayı kanopinin altına almak istersen: " +
                "DEMO > SodyumLamba nesnesini taşı.", root);
        }

        // ---------- Parçalar ----------

        /// <summary>Asfalt önalan + beton ada.</summary>
        private static void BuildForecourt(Transform parent)
        {
            var slab = Box(parent, "Beton_Ada", new Vector3(0f, 0.06f, 1.5f),
                new Vector3(7f, 0.12f, 3.2f), Concrete);
            slab.isStatic = true;

            // Zemini asfalta çevir — varsayılan gri fazla açık.
            var demo = parent.parent;
            var ground = demo != null ? demo.Find("Zemin") : null;
            if (ground != null)
            {
                var renderer = ground.GetComponent<MeshRenderer>();
                if (renderer != null) renderer.sharedMaterial = Mat("Bagisik_Asfalt", Asphalt, 0.9f);
            }
        }

        /// <summary>İstasyonun kanopisi — mekânı "yer" yapan en büyük hamle.</summary>
        private static void BuildCanopy(Transform parent)
        {
            var canopy = new GameObject("Kanopi");
            canopy.transform.SetParent(parent, false);

            // Tavan levhası
            Box(canopy.transform, "Tavan", new Vector3(0f, 4.6f, 1.5f),
                new Vector3(9f, 0.35f, 5.5f), Paint);
            // Alt yüzey biraz farklı ton — ışığı yakalasın
            Box(canopy.transform, "Tavan_Alt", new Vector3(0f, 4.4f, 1.5f),
                new Vector3(8.6f, 0.08f, 5.1f), Concrete);

            // Direkler
            float[] xs = { -3.6f, 3.6f };
            float[] zs = { -0.6f, 3.6f };
            int i = 0;
            foreach (float x in xs)
                foreach (float z in zs)
                    Box(canopy.transform, $"Direk_{++i}", new Vector3(x, 2.2f, z),
                        new Vector3(0.35f, 4.4f, 0.35f), Metal);
        }

        private static void BuildPumps(Transform parent)
        {
            var pumps = new GameObject("Pompalar");
            pumps.transform.SetParent(parent, false);

            BuildPump(pumps.transform, "Pompa_1", new Vector3(-1.8f, 0f, 1.5f), -6f);
            BuildPump(pumps.transform, "Pompa_2", new Vector3(1.8f, 0f, 1.5f), 4f);
        }

        private static void BuildPump(Transform parent, string name, Vector3 pos, float yaw)
        {
            var pump = new GameObject(name);
            pump.transform.SetParent(parent, false);
            pump.transform.localPosition = pos;
            pump.transform.localRotation = Quaternion.Euler(0f, yaw, 0f);

            Box(pump.transform, "Govde", new Vector3(0f, 0.85f, 0f),
                new Vector3(0.75f, 1.6f, 0.55f), RustMet);
            Box(pump.transform, "Ekran", new Vector3(0f, 1.35f, 0.3f),
                new Vector3(0.5f, 0.4f, 0.06f), Glass);
            Box(pump.transform, "Taban", new Vector3(0f, 0.1f, 0f),
                new Vector3(0.95f, 0.2f, 0.75f), Concrete);
            // Hortum kancası
            Box(pump.transform, "Kanca", new Vector3(0.42f, 1.05f, 0f),
                new Vector3(0.1f, 0.5f, 0.12f), Metal);
        }

        /// <summary>Arka planda dükkân cephesi — mekâna sınır ve derinlik verir.</summary>
        private static void BuildShop(Transform parent)
        {
            var shop = new GameObject("Dukkan");
            shop.transform.SetParent(parent, false);
            shop.transform.localPosition = new Vector3(0f, 0f, -6.5f);

            Box(shop.transform, "Bina", new Vector3(0f, 1.9f, 0f),
                new Vector3(11f, 3.8f, 3f), Concrete);

            // Vitrin camları — kırık/karanlık, içerisi boş
            for (int i = -2; i <= 2; i++)
            {
                Box(shop.transform, $"Vitrin_{i + 3}", new Vector3(i * 1.9f, 1.9f, 1.55f),
                    new Vector3(1.6f, 2.2f, 0.06f), Glass);
            }

            // Saçak
            Box(shop.transform, "Sacak", new Vector3(0f, 3.7f, 1.7f),
                new Vector3(11.4f, 0.25f, 0.9f), Paint);

            // Tabela direği
            Box(shop.transform, "Tabela_Direk", new Vector3(-6.2f, 2.6f, 2.5f),
                new Vector3(0.18f, 5.2f, 0.18f), Metal);
            Box(shop.transform, "Tabela", new Vector3(-6.2f, 5f, 2.5f),
                new Vector3(2.2f, 1.1f, 0.12f), RustMet);
        }

        /// <summary>Hurda araç — silüetiyle sahneye hikâye katar.</summary>
        private static void BuildWreck(Transform parent)
        {
            var car = new GameObject("Hurda_Arac");
            car.transform.SetParent(parent, false);
            car.transform.localPosition = new Vector3(5.2f, 0f, -1.5f);
            car.transform.localRotation = Quaternion.Euler(0f, 28f, 0f);

            Box(car.transform, "Govde", new Vector3(0f, 0.62f, 0f),
                new Vector3(1.85f, 0.62f, 4.2f), RustMet);
            Box(car.transform, "Kabin", new Vector3(0f, 1.18f, -0.25f),
                new Vector3(1.7f, 0.62f, 2.1f), Glass);
            Box(car.transform, "Kaput", new Vector3(0f, 0.9f, 1.9f),
                new Vector3(1.7f, 0.2f, 1.1f), RustMet);

            // Tekerlekler — biri düşmüş, terk edilmişlik hissi
            float[] wx = { -0.92f, 0.92f };
            float[] wz = { 1.35f, -1.35f };
            int i = 0;
            foreach (float x in wx)
                foreach (float z in wz)
                {
                    i++;
                    if (i == 3) continue; // eksik teker
                    var wheel = Cyl(car.transform, $"Teker_{i}", new Vector3(x, 0.34f, z),
                        new Vector3(0.34f, 0.14f, 0.34f), Metal);
                    wheel.transform.localRotation = Quaternion.Euler(0f, 0f, 90f);
                }
        }

        private static void BuildProps(Transform parent)
        {
            var props = new GameObject("Dekor");
            props.transform.SetParent(parent, false);

            // Variller — dağınık ama kümelenmiş, rastgele serpiştirilmiş değil
            Vector3[] barrels =
            {
                new Vector3(-6.4f, 0f, -2.2f), new Vector3(-5.7f, 0f, -2.9f),
                new Vector3(-6.1f, 0f, -3.6f), new Vector3(7.4f, 0f, 3.2f),
            };
            for (int i = 0; i < barrels.Length; i++)
            {
                var b = Cyl(props.transform, $"Varil_{i + 1}",
                    barrels[i] + Vector3.up * 0.44f,
                    new Vector3(0.34f, 0.44f, 0.34f), RustMet);
                b.transform.localRotation = Quaternion.Euler(0f, Random.Range(0f, 360f), 0f);
            }

            // Devrilmiş varil
            var fallen = Cyl(props.transform, "Varil_Devrik",
                new Vector3(-4.4f, 0.34f, -1.2f), new Vector3(0.34f, 0.44f, 0.34f), RustMet);
            fallen.transform.localRotation = Quaternion.Euler(90f, 24f, 0f);

            // Paletler ve kasalar
            Box(props.transform, "Palet_1", new Vector3(-7.2f, 0.08f, 2.4f),
                new Vector3(1.2f, 0.16f, 1.0f), Wood);
            Box(props.transform, "Kasa_1", new Vector3(-7.0f, 0.45f, 2.3f),
                new Vector3(0.8f, 0.7f, 0.7f), Wood).transform.localRotation =
                Quaternion.Euler(0f, 18f, 0f);
            Box(props.transform, "Kasa_2", new Vector3(6.6f, 0.35f, -4.2f),
                new Vector3(0.7f, 0.7f, 0.7f), Wood).transform.localRotation =
                Quaternion.Euler(0f, -12f, 0f);

            // Moloz — küçük, alçak, dağınık. Zeminin boş görünmesini engeller.
            for (int i = 0; i < 22; i++)
            {
                var p = new Vector3(Random.Range(-8.5f, 8.5f), 0f, Random.Range(-5f, 8f));
                float s = Random.Range(0.12f, 0.4f);
                var chunk = Box(props.transform, $"Moloz_{i + 1}",
                    p + Vector3.up * s * 0.5f,
                    new Vector3(s, s * Random.Range(0.3f, 0.7f), s * Random.Range(0.6f, 1.4f)),
                    i % 3 == 0 ? Wood : Concrete);
                chunk.transform.localRotation = Quaternion.Euler(
                    Random.Range(-8f, 8f), Random.Range(0f, 360f), Random.Range(-8f, 8f));
            }
        }

        // ---------- Yardımcılar ----------

        private static GameObject Box(Transform parent, string name, Vector3 pos,
                                      Vector3 size, Color color)
        {
            return Primitive(PrimitiveType.Cube, parent, name, pos, size, color);
        }

        private static GameObject Cyl(Transform parent, string name, Vector3 pos,
                                      Vector3 size, Color color)
        {
            return Primitive(PrimitiveType.Cylinder, parent, name, pos, size, color);
        }

        private static GameObject Primitive(PrimitiveType type, Transform parent, string name,
                                            Vector3 pos, Vector3 size, Color color)
        {
            var go = GameObject.CreatePrimitive(type);
            go.name = name;
            go.transform.SetParent(parent, false);
            go.transform.localPosition = pos;
            go.transform.localScale = size;
            go.GetComponent<MeshRenderer>().sharedMaterial = Mat(MatName(color), color, 0.88f);
            return go;
        }

        private static string MatName(Color c) => "Bagisik_" + ColorUtility.ToHtmlStringRGB(c);

        private static Material Mat(string name, Color color, float roughness)
        {
            const string dir = "Assets/Materials";
            if (!AssetDatabase.IsValidFolder(dir)) AssetDatabase.CreateFolder("Assets", "Materials");

            string path = $"{dir}/{name}.mat";
            var mat = AssetDatabase.LoadAssetAtPath<Material>(path);
            bool isNew = mat == null;
            if (isNew) mat = new Material(Shader.Find("Universal Render Pipeline/Lit")) { name = name };

            mat.SetColor("_BaseColor", color);
            mat.SetFloat("_Smoothness", Mathf.Clamp01(1f - roughness));
            mat.SetFloat("_Metallic", 0f);

            if (isNew) AssetDatabase.CreateAsset(mat, path);
            else EditorUtility.SetDirty(mat);
            return mat;
        }

        /// <summary>Statik işaretleme batching ve ışık pişirme için şart.</summary>
        private static void MarkStatic(GameObject root)
        {
            foreach (var t in root.GetComponentsInChildren<Transform>(true))
                t.gameObject.isStatic = true;
        }

        private static Color Hex(string hex)
        {
            return ColorUtility.TryParseHtmlString(hex, out var c) ? c : Color.magenta;
        }
    }
}
