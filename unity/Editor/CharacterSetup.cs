using System.Collections.Generic;
using System.IO;
using System.Linq;
using UnityEditor;
using UnityEditor.Animations;
using UnityEngine;

namespace Bagisik.EditorTools
{
    /// <summary>
    /// Mixamo'dan indirilen FBX'leri oyuna hazır hâle getirir:
    /// rig'leri Humanoid yapar, animasyonları karakterin avatarına bağlar,
    /// Speed parametreli bir blend tree kurar ve karakteri kapsülün yerine geçirir.
    ///
    /// Kullanım: Mixamo FBX'lerini Assets içine at, sonra bu komutu çalıştır.
    ///   • Karakter = içinde SkinnedMeshRenderer olan dosya ("With Skin" indirdiğin)
    ///   • Animasyon = geri kalanlar ("Without Skin")
    /// </summary>
    public static class CharacterSetup
    {
        private const string ControllerPath = "Assets/Settings/BagisikCharacter.controller";
        private const string SpeedParam = "Speed";

        [MenuItem("Bagisik/4 - Karakteri Kur", false, 40)]
        public static void Setup()
        {
            var models = FindModelImporters();
            if (models.Count == 0)
            {
                Tell("Assets içinde hiç FBX bulamadım.\n\n" +
                     "Mixamo'dan indirdiğin dosyaları Assets klasörüne sürükleyip tekrar dene.");
                return;
            }

            var characterPath = FindCharacterModel(models);
            if (characterPath == null)
            {
                Tell("Mesh içeren bir karakter dosyası bulamadım.\n\n" +
                     "Mixamo'da en az bir animasyonu 'With Skin' olarak indirmen gerekiyor — " +
                     "karakterin gövdesi o dosyada geliyor.");
                return;
            }

            var avatar = MakeHumanoid(characterPath);
            if (avatar == null)
            {
                Tell($"'{Path.GetFileName(characterPath)}' Humanoid'e çevrilemedi.\n\n" +
                     "Dosya insansı bir iskelet içermiyor olabilir.");
                return;
            }

            int bound = 0;
            foreach (var path in models.Where(p => p != characterPath))
                if (BindAnimationToAvatar(path, avatar)) bound++;

            var clips = CollectClips(models);
            var controller = BuildController(clips);
            var placed = PlaceCharacter(characterPath, controller);

            AssetDatabase.SaveAssets();

            Debug.Log(
                "[Bağışık] Karakter kuruldu.\n" +
                $"• Karakter: {Path.GetFileName(characterPath)}\n" +
                $"• Avatara bağlanan animasyon dosyası: {bound}\n" +
                $"• Klipler — idle: {Name(clips.idle)} · walk: {Name(clips.walk)} · run: {Name(clips.run)}\n" +
                $"• Animator Controller: {ControllerPath}\n" +
                (placed
                    ? "• Karakter Player'ın yerine geçti, kapsül gizlendi.\n\nPlay'e bas ve yürü."
                    : "• DEMO/Player bulunamadı — karakteri elle sahneye koyman gerekiyor."),
                controller);
        }

        // ---------- Bulma ----------

        private static List<string> FindModelImporters()
        {
            return AssetDatabase.FindAssets("t:Model", new[] { "Assets" })
                .Select(AssetDatabase.GUIDToAssetPath)
                .Where(p => p.EndsWith(".fbx", System.StringComparison.OrdinalIgnoreCase))
                .Distinct()
                .ToList();
        }

        /// <summary>Karakter = içinde gerçekten bir gövde (SkinnedMeshRenderer) olan dosya.</summary>
        private static string FindCharacterModel(List<string> paths)
        {
            return paths.FirstOrDefault(p =>
            {
                var go = AssetDatabase.LoadAssetAtPath<GameObject>(p);
                return go != null && go.GetComponentInChildren<SkinnedMeshRenderer>(true) != null;
            });
        }

        // ---------- Rig ----------

        private static Avatar MakeHumanoid(string path)
        {
            var importer = AssetImporter.GetAtPath(path) as ModelImporter;
            if (importer == null) return null;

            if (importer.animationType != ModelImporterAnimationType.Human ||
                importer.avatarSetup != ModelImporterAvatarSetup.CreateFromThisModel)
            {
                importer.animationType = ModelImporterAnimationType.Human;
                importer.avatarSetup = ModelImporterAvatarSetup.CreateFromThisModel;
                importer.SaveAndReimport();
            }

            return AssetDatabase.LoadAllAssetsAtPath(path).OfType<Avatar>().FirstOrDefault();
        }

        /// <summary>
        /// "Without Skin" indirilen animasyon dosyaları kendi iskeletini taşımaz;
        /// karakterin avatarına kopyalanmaları gerekir.
        /// </summary>
        private static bool BindAnimationToAvatar(string path, Avatar avatar)
        {
            var importer = AssetImporter.GetAtPath(path) as ModelImporter;
            if (importer == null) return false;

            if (importer.animationType == ModelImporterAnimationType.Human &&
                importer.sourceAvatar == avatar)
            {
                return true; // zaten bağlı
            }

            importer.animationType = ModelImporterAnimationType.Human;
            importer.avatarSetup = ModelImporterAvatarSetup.CopyFromOther;
            importer.sourceAvatar = avatar;
            importer.SaveAndReimport();
            return true;
        }

        // ---------- Klipler ----------

        private struct ClipSet
        {
            public AnimationClip idle;
            public AnimationClip walk;
            public AnimationClip run;
        }

        private static ClipSet CollectClips(List<string> modelPaths)
        {
            var all = new List<AnimationClip>();

            foreach (string path in modelPaths)
            {
                all.AddRange(AssetDatabase.LoadAllAssetsAtPath(path)
                    .OfType<AnimationClip>()
                    // Unity'nin dahili önizleme klipleri sayılmasın.
                    .Where(c => !c.name.StartsWith("__preview__")));
            }

            var set = new ClipSet
            {
                // Sıra önemli: birden fazla idle varsa sakin duruş kazansın.
                idle = Match(all, "standing idle", "breathing idle", "idle", "stand"),
                walk = Match(all, "walking", "walk"),
                run = Match(all, "slow run", "running", "run", "jog", "sprint"),
            };

            // Eksik olanı en yakın alternatifle doldur — blend tree boş kalmasın.
            if (set.idle == null) set.idle = all.FirstOrDefault();
            if (set.walk == null) set.walk = set.run ?? set.idle;
            if (set.run == null) set.run = set.walk;

            // Yürüme ve koşma döngüsel olmalı, yoksa her adımda takılır.
            MakeLooping(set.idle);
            MakeLooping(set.walk);
            MakeLooping(set.run);

            return set;
        }

        // Dövüş/silah duruşları hikâye sahnesinde yanlış okunur — elenir.
        private static readonly string[] Unwanted =
            { "combat", "fight", "boxing", "punch", "kick", "rifle", "pistol", "sword", "aim" };

        private static AnimationClip Match(List<AnimationClip> clips, params string[] keywords)
        {
            foreach (string keyword in keywords)
            {
                // Önce istenmeyenleri elenmiş hâliyle ara, bulunamazsa hepsinde ara.
                var hit = clips.FirstOrDefault(c => Matches(c, keyword) && !IsUnwanted(c))
                       ?? clips.FirstOrDefault(c => Matches(c, keyword));
                if (hit != null) return hit;
            }
            return null;
        }

        private static bool Matches(AnimationClip clip, string keyword)
        {
            return clip.name.ToLowerInvariant().Contains(keyword);
        }

        private static bool IsUnwanted(AnimationClip clip)
        {
            string name = clip.name.ToLowerInvariant();
            return Unwanted.Any(name.Contains);
        }

        private static void MakeLooping(AnimationClip clip)
        {
            if (clip == null) return;

            var settings = AnimationUtility.GetAnimationClipSettings(clip);
            if (settings.loopTime) return;

            settings.loopTime = true;
            AnimationUtility.SetAnimationClipSettings(clip, settings);
            EditorUtility.SetDirty(clip);
        }

        // ---------- Animator ----------

        private static AnimatorController BuildController(ClipSet clips)
        {
            EnsureFolder("Assets/Settings");

            var existing = AssetDatabase.LoadAssetAtPath<AnimatorController>(ControllerPath);
            if (existing != null) AssetDatabase.DeleteAsset(ControllerPath);

            var controller = AnimatorController.CreateAnimatorControllerAtPath(ControllerPath);
            controller.AddParameter(SpeedParam, AnimatorControllerParameterType.Float);

            // Blend tree elle kuruluyor: CreateBlendTreeInController'ın imzası
            // Unity sürümleri arasında değişiyor, bu yol her sürümde aynı çalışır.
            // PlayerMover normalize edilmiş hızı (0-1) besliyor:
            // 0 = duruyor, ~0.5 = yürüyor, 1 = koşuyor.
            var tree = new BlendTree
            {
                name = "Locomotion",
                blendParameter = SpeedParam,
                blendType = BlendTreeType.Simple1D,
                useAutomaticThresholds = false,
                hideFlags = HideFlags.HideInHierarchy,
            };

            var children = new List<ChildMotion>();
            void Add(Motion motion, float threshold)
            {
                if (motion == null) return;
                children.Add(new ChildMotion { motion = motion, threshold = threshold, timeScale = 1f });
            }

            Add(clips.idle, 0f);
            Add(clips.walk, 0.5f);
            if (clips.run != clips.walk) Add(clips.run, 1f);
            tree.children = children.ToArray();

            // Controller'ın alt varlığı olarak kaydet, yoksa sahne yeniden
            // açıldığında blend tree kaybolur.
            AssetDatabase.AddObjectToAsset(tree, controller);

            var stateMachine = controller.layers[0].stateMachine;
            var state = stateMachine.AddState("Locomotion");
            state.motion = tree;
            state.writeDefaultValues = false;
            stateMachine.defaultState = state;

            EditorUtility.SetDirty(controller);
            return controller;
        }

        // ---------- Sahneye yerleştirme ----------

        private static bool PlaceCharacter(string modelPath, AnimatorController controller)
        {
            var demo = GameObject.Find("DEMO");
            var player = demo != null ? demo.transform.Find("Player") : null;
            if (player == null) return false;

            const string childName = "Karakter";
            var old = player.Find(childName);
            if (old != null) Object.DestroyImmediate(old.gameObject);

            var prefab = AssetDatabase.LoadAssetAtPath<GameObject>(modelPath);
            var instance = (GameObject)PrefabUtility.InstantiatePrefab(prefab, player);
            instance.name = childName;

            // CharacterController'ın merkezi kapsülün ortasında; model ayaklarından
            // başladığı için yarım boy aşağı kaydırılır.
            instance.transform.localPosition = new Vector3(0f, -1f, 0f);
            instance.transform.localRotation = Quaternion.identity;

            var animator = instance.GetComponent<Animator>() ?? instance.AddComponent<Animator>();
            animator.runtimeAnimatorController = controller;
            animator.applyRootMotion = false; // hareketi PlayerMover sürüyor

            // Kapsülü gizle ama silme — çarpışma kutusunun ölçüsü olarak dursun.
            var capsuleRenderer = player.GetComponent<MeshRenderer>();
            if (capsuleRenderer != null) capsuleRenderer.enabled = false;

            TonedownCharacterMaterials(instance);

            var mover = player.GetComponent<PlayerMover>();
            if (mover != null)
            {
                var so = new SerializedObject(mover);
                so.FindProperty("animator").objectReferenceValue = animator;
                so.ApplyModifiedPropertiesWithoutUndo();
            }

            Selection.activeGameObject = player.gameObject;
            return true;
        }

        /// <summary>
        /// Mixamo materyalleri parlak ve doygun gelir; sahnedeki en aydınlık
        /// nesne karakter olursa kompozisyon bozulur ve palet disiplini kırılır.
        /// Dokuyu koruyup albedo'yu karartıp matlaştırıyoruz.
        /// </summary>
        private static void TonedownCharacterMaterials(GameObject character)
        {
            foreach (var renderer in character.GetComponentsInChildren<Renderer>(true))
            {
                foreach (var mat in renderer.sharedMaterials)
                {
                    if (mat == null) continue;

                    if (mat.HasProperty("_BaseColor"))
                        mat.SetColor("_BaseColor", new Color(0.52f, 0.5f, 0.47f));
                    else if (mat.HasProperty("_Color"))
                        mat.SetColor("_Color", new Color(0.52f, 0.5f, 0.47f));

                    // Parlak ten/kumaş plastik görünümün baş sebebi.
                    if (mat.HasProperty("_Smoothness")) mat.SetFloat("_Smoothness", 0.12f);
                    if (mat.HasProperty("_Glossiness")) mat.SetFloat("_Glossiness", 0.12f);
                    if (mat.HasProperty("_Metallic")) mat.SetFloat("_Metallic", 0f);

                    EditorUtility.SetDirty(mat);
                }
            }
        }

        // ---------- Yardımcılar ----------

        private static void EnsureFolder(string path)
        {
            if (AssetDatabase.IsValidFolder(path)) return;
            AssetDatabase.CreateFolder(Path.GetDirectoryName(path).Replace('\\', '/'),
                                       Path.GetFileName(path));
        }

        private static string Name(AnimationClip clip) => clip != null ? clip.name : "yok";

        private static void Tell(string message)
        {
            EditorUtility.DisplayDialog("Karakter kurulumu", message, "Tamam");
        }
    }
}
