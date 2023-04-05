package com.nisovin.magicspells.util.wrapper;

import lombok.Getter;


public final class WrappedMaterial {
    private WrappedMaterial() {
        throw new RuntimeException();
    }
    @Getter
    private static IMaterialGetter wrapper;

    static  {
        switch (MaterialWrapper.getVersion()) {
            case LEGACY:
                wrapper = new LegacyMaterial();
                break;
            case CURRENT:
                wrapper = new CurrentMaterial();
                break;
            case UNKNOWN:
                wrapper = null;
                break;
        }
    }

}
