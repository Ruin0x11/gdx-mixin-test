package xyz.ruin.gdxtest.mod2.mixin;

import com.badlogic.gdx.ScreenAdapter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.ruin.gdxtest.core.TiledMapScreen;

@Mixin(TiledMapScreen.class)
public class TestMixinTiledMapScreen2 extends ScreenAdapter {
    @Inject(method = "testPrintThing", at = @At("HEAD"))
    protected void onTestPrintThing2(String dood, CallbackInfoReturnable<String> cir) {
        System.out.println("I'm a callback");
    }
}
