package ch.epfl.gameboj.gui;

import ch.epfl.gameboj.GameBoy;
import ch.epfl.gameboj.component.Joypad;
import ch.epfl.gameboj.component.cartridge.Cartridge;
import ch.epfl.gameboj.component.lcd.LcdController;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.event.EventHandler;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * The emulator's Main class
 *
 * @author Matthieu De Beule (Sciper: 269623)
 * @author Andrew Dobis (Sciper: 272002)
 */
public final class Main extends Application {
    private static final int GUI_WIDTH = LcdController.LCD_WIDTH * 2;
    private static final int GUI_HEIGHT = LcdController.LCD_HEIGHT * 2;
    public static void main(String[] args) {
        Application.launch(args);
    }

    /**
     * The main entry point for all JavaFX applications.
     * The start method is called after the init method has returned,
     * and after the system is ready for the application to begin running.
     *
     * <p>
     * NOTE: This method is called on the JavaFX Application Thread.
     * </p>
     *
     * @param primaryStage the primary stage for this application, onto which
     *                     the application scene can be set.
     *                     Applications may create other stages, if needed, but they will not be
     *                     primary stages.
     * @throws Exception if something goes wrong
     */
    @Override public void start(Stage primaryStage) throws Exception {

        if(getParameters().getRaw().size() != 1){
            System.out.println("Must give exactly one argument");
            System.exit(1);
        }

        String parameter = getParameters().getRaw().get(0);
        Cartridge cartridge = Cartridge.ofFile(new File(parameter));
        GameBoy gameBoy = new GameBoy(cartridge);

        ImageView imageView = new ImageView();
        imageView.setFitWidth(GUI_WIDTH * 2);
        imageView.setFitHeight(GUI_HEIGHT * 2);

        //Mapping of the arrow keys
        Map<KeyCode, Joypad.Key> joypadMapKeyCode = new HashMap<>();
        joypadMapKeyCode.put(KeyCode.UP, Joypad.Key.UP);
        joypadMapKeyCode.put(KeyCode.DOWN, Joypad.Key.DOWN);
        joypadMapKeyCode.put(KeyCode.RIGHT, Joypad.Key.RIGHT);
        joypadMapKeyCode.put(KeyCode.LEFT, Joypad.Key.LEFT);

        //Mapping of the other keys ( both lower and upper case inputs work )
        Map<String, Joypad.Key> joypadMapChar = new HashMap<>();
        joypadMapChar.put("a", Joypad.Key.A);
        joypadMapChar.put("A", Joypad.Key.A);
        joypadMapChar.put("b", Joypad.Key.B);
        joypadMapChar.put("B", Joypad.Key.B);
        joypadMapChar.put("s", Joypad.Key.START);
        joypadMapChar.put("S", Joypad.Key.START);
        joypadMapChar.put(" ", Joypad.Key.SELECT);

        EventHandler<KeyEvent> keyPressed = (key -> {
            if (joypadMapKeyCode.containsKey(key.getCode())){
                gameBoy.joypad().keyPressed(joypadMapKeyCode.get(key.getCode()));
            }
            else if (joypadMapChar.containsKey(key.getText())){
                gameBoy.joypad().keyPressed(joypadMapChar.get(key.getText()));
            }

            //to exit the emulator
            if(key.getCode() == KeyCode.ESCAPE) {
                System.exit(1);
            }

            if(key.getCode() == KeyCode.F5) {
                try {
                    cartridge.saveState();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if(key.getCode() == KeyCode.F6) {
                try {
                    cartridge.loadState();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        EventHandler<KeyEvent> keyReleased = (key -> {
            if (joypadMapKeyCode.containsKey(key.getCode())){
                gameBoy.joypad().keyReleased(joypadMapKeyCode.get(key.getCode()));
            }
            else if (joypadMapChar.containsKey(key.getText())){
                gameBoy.joypad().keyReleased(joypadMapChar.get(key.getText()));
            }
        });

        imageView.setOnKeyPressed(keyPressed);
        imageView.setOnKeyReleased(keyReleased);
        BorderPane border = new BorderPane(imageView);
        Scene scene = new Scene(border);

        primaryStage.setScene(scene);
        primaryStage.setTitle("Gameboj");

        primaryStage.show();
        imageView.requestFocus();

        long start = System.nanoTime();

        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                long elapsedCycles = (long) ((now - start) * GameBoy.CYCLES_PER_NANOSECOND);
                gameBoy.runUntil(elapsedCycles);
                imageView.setImage(ImageConverter.convert(gameBoy.
                        lcdController().currentImage()));
            }
        };
        timer.start();
    }
}
