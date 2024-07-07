package net.idothehax.firstpersonnom;

import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FirstPersonNom implements ModInitializer {
    public static final String MOD_ID = "firstpersonnom";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing FirstPersonNom");
    }
}
