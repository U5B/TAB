package me.neznamy.tab.shared.features.layout.skin;

import me.neznamy.tab.shared.config.file.ConfigurationFile;
import me.neznamy.tab.shared.platform.TabList.Skin;
import me.neznamy.tab.shared.util.cache.Cache;
import org.jetbrains.annotations.NotNull;

/**
 * Skin source using raw texture;signature.
 */
public class SignedTexture extends SkinSource {
    @NotNull protected final Cache<String, Skin> textures = new Cache<>("SignedTexture Cache", 250, this::download);

    protected SignedTexture(@NotNull ConfigurationFile file) {
        super(file, "signed_textures");
    }

    @Override
    @NotNull
    public Skin download(@NotNull String textureBase64) {
        String[] parts = textureBase64.split(";");
        String base64 = parts[0];
        String signature = parts.length > 1 ? parts[1] : "";
        return new Skin(base64, signature);
    }

    @Override
    public Skin getSkin(@NotNull String skin) {
        if (skins.containsKey(skin)) {
            return skins.get(skin);
        }
        return textures.get(skin);
    }

    @Override
    public void unload() {
        // do not save SignedTexture to file
    }
}
