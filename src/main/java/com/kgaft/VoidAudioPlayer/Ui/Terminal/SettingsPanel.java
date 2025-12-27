package com.kgaft.VoidAudioPlayer.Ui.Terminal;

import com.googlecode.lanterna.gui2.*;
import com.googlecode.lanterna.gui2.dialogs.TextInputDialog;
import com.googlecode.lanterna.gui2.dialogs.TextInputDialogBuilder;
import com.kgaft.VoidAudioPlayer.Model.MSettings;
import com.kgaft.VoidAudioPlayer.Ui.Terminal.SelectorPanel.SelectorPanel;

import java.io.File;

enum ActionResult{
    DSD_DEVICE_SELECTED,
    GST_DEVICE_SELECTED,
    DB_PATH_ENTERED
}

public class SettingsPanel extends Panel implements IOptionConsumer {
    private Label databasePathLabel;
    private Label dsdDevice;
    private Label gstDevice;
    private Button exitButton;
    private ActionListBox listBox;
    private MSettings currentSettings;
    private Window parentWindow;
    private Panel componentToReturn;

    public SettingsPanel(Window parentWindow, Panel componentToReturn) {
        this.parentWindow = parentWindow;
        this.componentToReturn = componentToReturn;
        refreshPage();
    }
    private void refreshPage(){
        removeAllComponents();
        currentSettings = MSettings.readSettingsOrDefault();
        databasePathLabel = new Label("Database path: "+currentSettings.getCollectionDBPath());
        exitButton = new Button("Back", ()->{
            parentWindow.setComponent(componentToReturn);
        });
        dsdDevice = new Label("DSD Device: "+currentSettings.getDefaultDsdPlaybackDeviceName());
        gstDevice = new Label("GStreamer device: " + currentSettings.getDefaultGstPlaybackDevice());
        listBox = new ActionListBox();
        listBox.addItem("Select dsd device --->", () -> {
            new SelectorPanel(parentWindow, ActionResult.DSD_DEVICE_SELECTED,this, this, this,"Here is available dsd devices: ", currentSettings.showAvailableDsdDevices().toArray());
        });
        listBox.addItem("Select GStreamer device --->", () -> {
            new SelectorPanel(parentWindow, ActionResult.GST_DEVICE_SELECTED, this, this,this,"Select GStreamer device", currentSettings.showAvailableGstDevices().toArray());
        });
        listBox.addItem("Set path to collection database --->", ()->{
            TextInputDialog textInput = new TextInputDialogBuilder().setPasswordInput(false).setTitle("Set path to collection database").build();
            String input = textInput.showDialog(parentWindow.getTextGUI(), "Set path to collection database", "", currentSettings.getCollectionDBPath());
            if(input!=null && !input.isEmpty()){
                File file = new File(input);
                if(new File(file.getParent()).exists()){
                    currentSettings.setCollectionDBPath(file.getAbsolutePath());
                    MSettings.saveCurrentSettings();
                    refreshPage();
                }
            }
        });
        addComponent(databasePathLabel);
        addComponent(dsdDevice);
        addComponent(gstDevice);
        addComponent(listBox);
        addComponent(exitButton);
    }
    @Override
    public void optionSelected(long id, Object userData, Object option) {
        ActionResult result = (ActionResult) userData;
        switch (result){
            case DSD_DEVICE_SELECTED -> {
                currentSettings.setDsdDevice(option.toString());
                break;
            }
            case GST_DEVICE_SELECTED -> {
                currentSettings.setGstDevice(option.toString());
                break;
            }
        }
        MSettings.saveCurrentSettings();
        refreshPage();
    }
}
