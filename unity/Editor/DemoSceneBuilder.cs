using UnityEditor;
using UnityEngine;
using UnityEngine.EventSystems;
using UnityEngine.InputSystem.OnScreen;
using UnityEngine.InputSystem.UI;
using UnityEngine.UI;

namespace Bagisik.EditorTools
{
    /// <summary>
    /// Demo sahnesini tek menü tıklamasıyla kurar: zemin, duvarlar, engeller,
    /// ışık, oyuncu, kamera ve dokunmatik joystick.
    ///
    /// Elle tıklamayı ortadan kaldırmak için var — değerler
    /// docs/SANAT-YONU.md ve docs/DEMO.md ile uyumlu.
    /// </summary>
    public static class DemoSceneBuilder
    {
        private const string Root = "DEMO";
        private const float HalfSize = 10f;   // oyun alanının yarı genişliği (metre)
        private const float WallHeight = 3f;

        [MenuItem("Bagisik/2 - Demo Sahnesini Kur", false, 20)]
        public static void Build()
        {
            if (GameObject.Find(Root) != null &&
                !EditorUtility.DisplayDialog(
                    "Demo sahnesi zaten var",
                    "Sahnede bir DEMO nesnesi bulundu. Yeniden kurulsun mu? " +
                    "Mevcut DEMO nesnesi ve içindekiler silinecek.",
                    "Yeniden kur", "Vazgeç"))
            {
                return;
            }

            var existing = GameObject.Find(Root);
            if (existing != null) Undo.DestroyObjectImmediate(existing);

            var root = new GameObject(Root);
            Undo.RegisterCreatedObjectUndo(root, "Demo sahnesini kur");

            BuildGround(root.transform);
            BuildWalls(root.transform);
            BuildObstacles(root.transform);
            var light = EnsureDirectionalLight();
            var player = BuildPlayer(root.transform);
            WireCamera(player.transform);
            BuildTouchUI(root.transform);
            EnsureEventSystem();

            Selection.activeGameObject = player;
            EditorApplication.ExecuteMenuItem("Edit/Frame Selected");

            Debug.Log(
                "[Bağışık] Demo sahnesi kuruldu.\n" +
                "• Play'e bas, WASD ile yürü (editörde) veya ekrandaki joystick'i kullan.\n" +
                "• Hisse göre ayar: Player > Player Mover (acceleration, turnSpeed), " +
                "Main Camera > Camera Rig (offset, followSmoothTime).\n" +
                "• Kaydetmeyi unutma: Ctrl+S.", light);
        }

        // ---------- Parçalar ----------

        private static void BuildGround(Transform parent)
        {
            var ground = GameObject.CreatePrimitive(PrimitiveType.Plane);
            ground.name = "Zemin";
            ground.transform.SetParent(parent, false);
            // Plane varsayılanı 10x10 birim; x2 = 20x20 m oyun alanı.
            ground.transform.localScale = new Vector3(HalfSize / 5f, 1f, HalfSize / 5f);
            ground.isStatic = true;
        }

        private static void BuildWalls(Transform parent)
        {
            var walls = new GameObject("Duvarlar");
            walls.transform.SetParent(parent, false);

            // Alanı dört yandan kapat — "sınırlı serbest yürüme", açık dünya değil.
            AddWall(walls.transform, "Duvar_Kuzey", new Vector3(0f, WallHeight / 2f, HalfSize),
                new Vector3(HalfSize * 2f, WallHeight, 0.5f));
            AddWall(walls.transform, "Duvar_Guney", new Vector3(0f, WallHeight / 2f, -HalfSize),
                new Vector3(HalfSize * 2f, WallHeight, 0.5f));
            AddWall(walls.transform, "Duvar_Dogu", new Vector3(HalfSize, WallHeight / 2f, 0f),
                new Vector3(0.5f, WallHeight, HalfSize * 2f));
            AddWall(walls.transform, "Duvar_Bati", new Vector3(-HalfSize, WallHeight / 2f, 0f),
                new Vector3(0.5f, WallHeight, HalfSize * 2f));
        }

        private static void AddWall(Transform parent, string name, Vector3 pos, Vector3 scale)
        {
            var wall = GameObject.CreatePrimitive(PrimitiveType.Cube);
            wall.name = name;
            wall.transform.SetParent(parent, false);
            wall.transform.localPosition = pos;
            wall.transform.localScale = scale;
            wall.isStatic = true;
        }

        private static void BuildObstacles(Transform parent)
        {
            var obstacles = new GameObject("Engeller");
            obstacles.transform.SetParent(parent, false);

            // Çarpışmayı, kamera duvar-kaçınmasını ve mesafe hissini test etmek için.
            Vector3[] spots =
            {
                new Vector3(3.5f, 0.5f, 2f),
                new Vector3(-4f, 0.75f, -3f),
                new Vector3(1f, 1f, -6f),
                new Vector3(-6f, 0.5f, 5f),
            };
            float[] heights = { 1f, 1.5f, 2f, 1f };

            for (int i = 0; i < spots.Length; i++)
            {
                var box = GameObject.CreatePrimitive(PrimitiveType.Cube);
                box.name = $"Engel_{i + 1}";
                box.transform.SetParent(obstacles.transform, false);
                box.transform.localPosition = spots[i];
                box.transform.localScale = new Vector3(1.2f, heights[i], 1.2f);
                box.transform.localRotation = Quaternion.Euler(0f, i * 27f, 0f);
                box.isStatic = true;
            }
        }

        private static Light EnsureDirectionalLight()
        {
            var existing = Object.FindFirstObjectByType<Light>();
            if (existing != null && existing.type == LightType.Directional)
                return existing;

            var go = new GameObject("Directional Light");
            Undo.RegisterCreatedObjectUndo(go, "Işık ekle");
            var light = go.AddComponent<Light>();
            light.type = LightType.Directional;
            light.transform.rotation = Quaternion.Euler(50f, -30f, 0f);
            return light;
        }

        private static GameObject BuildPlayer(Transform parent)
        {
            var player = GameObject.CreatePrimitive(PrimitiveType.Capsule);
            player.name = "Player";
            player.transform.SetParent(parent, false);
            player.transform.localPosition = new Vector3(0f, 1f, 0f);

            // CharacterController çarpışmayı kendi yönetir; primitive'in
            // collider'ı kalırsa ikisi çakışır.
            var primitiveCollider = player.GetComponent<Collider>();
            if (primitiveCollider != null) Object.DestroyImmediate(primitiveCollider);

            var controller = player.AddComponent<CharacterController>();
            controller.height = 2f;
            controller.radius = 0.35f;
            controller.center = Vector3.zero;
            controller.slopeLimit = 45f;
            controller.stepOffset = 0.3f;

            player.AddComponent<PlayerMover>();
            player.AddComponent<HotspotDetector>();

            return player;
        }

        private static void WireCamera(Transform target)
        {
            var cam = Camera.main;
            if (cam == null)
            {
                var go = new GameObject("Main Camera") { tag = "MainCamera" };
                Undo.RegisterCreatedObjectUndo(go, "Kamera ekle");
                cam = go.AddComponent<Camera>();
                go.AddComponent<AudioListener>();
            }

            var rig = cam.GetComponent<CameraRig>();
            if (rig == null) rig = Undo.AddComponent<CameraRig>(cam.gameObject);

            // target private [SerializeField] — SerializedObject üzerinden bağlanır.
            var so = new SerializedObject(rig);
            so.FindProperty("target").objectReferenceValue = target;
            so.ApplyModifiedPropertiesWithoutUndo();

            rig.SnapToTarget();
        }

        private static void BuildTouchUI(Transform parent)
        {
            var canvasGo = new GameObject("UI",
                typeof(Canvas), typeof(CanvasScaler), typeof(GraphicRaycaster));
            canvasGo.transform.SetParent(parent, false);
            canvasGo.layer = LayerMask.NameToLayer("UI");

            var canvas = canvasGo.GetComponent<Canvas>();
            canvas.renderMode = RenderMode.ScreenSpaceOverlay;

            // Olmazsa joystick bazı telefonlarda minicik, bazılarında devasa görünür.
            var scaler = canvasGo.GetComponent<CanvasScaler>();
            scaler.uiScaleMode = CanvasScaler.ScaleMode.ScaleWithScreenSize;
            scaler.referenceResolution = new Vector2(1920f, 1080f);
            scaler.screenMatchMode = CanvasScaler.ScreenMatchMode.MatchWidthOrHeight;
            scaler.matchWidthOrHeight = 0.5f;

            var knob = AssetDatabase.GetBuiltinExtraResource<Sprite>("UI/Skin/Knob.psd");

            var baseGo = new GameObject("JoystickBase", typeof(Image));
            baseGo.transform.SetParent(canvasGo.transform, false);
            baseGo.layer = canvasGo.layer;
            var baseImage = baseGo.GetComponent<Image>();
            baseImage.sprite = knob;
            baseImage.color = new Color(1f, 1f, 1f, 0.22f);
            baseImage.raycastTarget = false;

            var baseRect = baseGo.GetComponent<RectTransform>();
            baseRect.anchorMin = baseRect.anchorMax = baseRect.pivot = new Vector2(0f, 0f);
            baseRect.anchoredPosition = new Vector2(230f, 210f);
            baseRect.sizeDelta = new Vector2(300f, 300f);

            var handleGo = new GameObject("JoystickHandle", typeof(Image));
            handleGo.transform.SetParent(baseGo.transform, false);
            handleGo.layer = canvasGo.layer;
            var handleImage = handleGo.GetComponent<Image>();
            handleImage.sprite = knob;
            handleImage.color = new Color(1f, 1f, 1f, 0.55f);

            var handleRect = handleGo.GetComponent<RectTransform>();
            handleRect.anchorMin = handleRect.anchorMax = handleRect.pivot = new Vector2(0.5f, 0.5f);
            handleRect.anchoredPosition = Vector2.zero;
            handleRect.sizeDelta = new Vector2(130f, 130f);

            // OnScreenStick sanal bir gamepad besler; PlayerMover onu okur,
            // yani ayrıca bir Input Action bağlamaya gerek yok.
            var stick = handleGo.AddComponent<OnScreenStick>();
            var stickSo = new SerializedObject(stick);
            stickSo.FindProperty("m_ControlPath").stringValue = "<Gamepad>/leftStick";
            var range = stickSo.FindProperty("m_MovementRange");
            if (range != null) range.floatValue = 85f;
            stickSo.ApplyModifiedPropertiesWithoutUndo();
        }

        private static void EnsureEventSystem()
        {
            if (Object.FindFirstObjectByType<EventSystem>() != null) return;

            var go = new GameObject("EventSystem", typeof(EventSystem));
            Undo.RegisterCreatedObjectUndo(go, "EventSystem ekle");
            // Yeni Input System kullanıldığı için StandaloneInputModule değil bu.
            go.AddComponent<InputSystemUIInputModule>();
        }
    }
}
