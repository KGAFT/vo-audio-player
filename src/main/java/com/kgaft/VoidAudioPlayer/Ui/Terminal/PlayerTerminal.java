package com.kgaft.VoidAudioPlayer.Ui.Terminal;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.gui2.*;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;
import com.kgaft.VoidAudioPlayer.Ui.Terminal.CollectionPanel.CollectionPanel;

import java.io.IOException;

public class PlayerTerminal {
    private Screen screen;

    public PlayerTerminal() throws IOException {
        Terminal terminal = new DefaultTerminalFactory().createTerminal();
        screen = new TerminalScreen(terminal);
        screen.startScreen();

        MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
        BasicWindow window = new BasicWindow("Action List");

        Panel panel = new Panel();

        ActionListBox list = new ActionListBox(new TerminalSize(20, 10));
        list.addItem("Settings", () -> window.setComponent(new SettingsPanel(window, panel)));
        list.addItem("Collection", () -> window.setComponent(new CollectionPanel(window, panel)));

        panel.addComponent(list);
        panel.addComponent(new Button("Exit", window::close));

        window.setComponent(panel);
        gui.addWindowAndWait(window);
        screen.stopScreen();
    }
}
