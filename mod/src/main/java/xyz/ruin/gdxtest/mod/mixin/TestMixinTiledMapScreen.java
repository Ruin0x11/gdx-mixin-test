package xyz.ruin.gdxtest.mod.mixin;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.ruin.gdxtest.core.TiledMapScreen;

@Mixin(TiledMapScreen.class)
public class TestMixinTiledMapScreen extends ScreenAdapter {
    @Inject(method = "testPrintThing", at = @At("HEAD"), cancellable = false)
    protected void onTestPrintThing(String dood, CallbackInfoReturnable<String> cir) {
        System.out.println("yo i'm in yr callback");
    }

    @Inject(method = "update", at = @At("HEAD"))
    protected void onUpdateExtraKeys(float delta, CallbackInfo ci) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.D)) {
            System.out.println("Doadsd!");
        }
    }
}
