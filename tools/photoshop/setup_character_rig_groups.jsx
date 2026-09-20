#target photoshop

(function () {
    if (app.documents.length === 0) {
        alert("没有打开的 Photoshop 文档。");
        return;
    }

    var doc = app.activeDocument;
    if (doc.name.toLowerCase() !== "character_rig.psd") {
        alert("安全停止：当前文档不是 Character_rig.psd，而是 " + doc.name);
        return;
    }

    var reserved = {
        "[ignore] 00_REFERENCE": true,
        "10_BACK_HAIR": true,
        "20_TORSO": true,
        "30_LEGS": true,
        "40_SKIRT": true,
        "50_WEAPON": true,
        "60_ARMS_HANDS": true,
        "70_FRONT_BODY": true,
        "80_FACE": true,
        "90_FRONT_HAIR": true
    };

    function findTopLevelGroup(name) {
        for (var i = 0; i < doc.layerSets.length; i++) {
            if (doc.layerSets[i].name === name) return doc.layerSets[i];
        }
        return null;
    }

    function ensureTopLevelGroup(name) {
        var group = findTopLevelGroup(name);
        if (group) return group;
        group = doc.layerSets.add();
        group.name = name;
        return group;
    }

    function setupRig() {
        var reference = ensureTopLevelGroup("[ignore] 00_REFERENCE");
        reference.allLocked = false;

        // Snapshot original top-level layers before moving them. Iterate bottom to
        // top and place at the beginning to preserve their visible stacking order.
        var originals = [];
        for (var i = 0; i < doc.layers.length; i++) {
            var layer = doc.layers[i];
            if (!reserved[layer.name]) originals.push(layer);
        }
        for (var j = originals.length - 1; j >= 0; j--) {
            originals[j].move(reference, ElementPlacement.PLACEATBEGINNING);
        }

        var groupsBottomToTop = [
            "10_BACK_HAIR",
            "20_TORSO",
            "30_LEGS",
            "40_SKIRT",
            "50_WEAPON",
            "60_ARMS_HANDS",
            "70_FRONT_BODY",
            "80_FACE",
            "90_FRONT_HAIR"
        ];
        for (var k = 0; k < groupsBottomToTop.length; k++) {
            ensureTopLevelGroup(groupsBottomToTop[k]);
        }

        reference.allLocked = true;
    }

    doc.suspendHistory("建立 Character Spine 拆件组", "setupRig()");
    doc.save();
    alert("Character_rig.psd 图层组已建立并保存。参考组已锁定。");
}());
