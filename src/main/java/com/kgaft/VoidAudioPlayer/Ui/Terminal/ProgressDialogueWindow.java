package com.kgaft.VoidAudioPlayer.Ui.Terminal;

import com.googlecode.lanterna.gui2.BasicWindow;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.ProgressBar;
import com.kgaft.VoidAudioPlayer.Ui.ProgressAcceptor;

public class ProgressDialogueWindow extends BasicWindow implements ProgressAcceptor {
    private Panel rootPanel;
    private Label currentState = new Label("");
    private ProgressBar progressBar = new ProgressBar(0, 100);
    public ProgressDialogueWindow(String title){
        super(title);
        rootPanel.addComponent(currentState);
        rootPanel.addComponent(progressBar);
        setComponent(rootPanel);
    }

    @Override
    public void setProgress(float progress) {
        progressBar.setValue((int) (progress*100));
    }

    @Override
    public void setCurrentState(String state) {
        currentState.setText(state);
    }
}
