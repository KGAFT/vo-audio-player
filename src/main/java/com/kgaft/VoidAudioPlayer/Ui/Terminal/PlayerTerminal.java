package com.kgaft.VoidAudioPlayer.Ui.Terminal;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.gui2.*;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;
import com.kgaft.VoidAudioPlayer.Ui.Terminal.CollectionPanel.CollectionPanel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PlayerTerminal {
    private Screen screen;
    private MultiWindowTextGUI gui;
    private List<Window> controlledWindows = new ArrayList<>();

    public PlayerTerminal() throws IOException {
        Terminal terminal = new DefaultTerminalFactory().createTerminal();
        screen = new TerminalScreen(terminal);
        screen.startScreen();

        gui = new MultiWindowTextGUI(screen);

    }

    public void registerWindow(Window window){
        controlledWindows.add(window);
    }

    public synchronized void start(){
        try{
            controlledWindows.forEach(window -> {
                gui.waitForWindowToClose(window);
            });
            screen.stopScreen();
        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    public Screen getScreen() {
        return screen;
    }

    public MultiWindowTextGUI getGui() {
        return gui;
    }
}
