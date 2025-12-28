package com.kgaft.VoidAudioPlayer.Ui.Terminal;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.gui2.ActionListBox;
import com.googlecode.lanterna.gui2.BasicWindow;
import com.googlecode.lanterna.gui2.Button;
import com.googlecode.lanterna.gui2.Panel;
import com.kgaft.VoidAudioPlayer.Ui.Terminal.CollectionPanel.CollectionPanel;

public class PlayerMenuWindow {
    public PlayerMenuWindow(PlayerTerminal terminal){
        BasicWindow window = new BasicWindow("Action List");

        Panel panel = new Panel();

        ActionListBox list = new ActionListBox(new TerminalSize(20, 10));
        list.addItem("Settings", () -> window.setComponent(new SettingsPanel(window, panel)));
        list.addItem("Collection", () -> window.setComponent(new CollectionPanel(window, panel)));
        list.addItem("Player controls", () -> new PlayerWindow(terminal));

        panel.addComponent(list);
        panel.addComponent(new Button("Exit", window::close));

        window.setComponent(panel);
        terminal.getGui().addWindow(window);
        terminal.registerWindow(window);
    }
}
