package com.kgaft.VoidAudioPlayer.Ui;


import com.kgaft.VoidAudioPlayer.Native.Player;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.concurrent.Semaphore;

public class TrackControlFlow extends JSlider implements Runnable {
    private Semaphore sliderSemaphore = new Semaphore(1);
    private volatile boolean userChangingPos = false;
    private volatile boolean trackPosUpdated = false;
    private volatile float posSkew = 0;
    private Thread controlThread;
    private boolean isRunning = false;
    private long playerHandle;

    public TrackControlFlow(long playerHandle) {
        super(0, 100, 0);
        this.playerHandle = playerHandle;
        setLayout(new GridLayout(1, 1));
        addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                double dx = e.getX();
                double width = getWidth();
                posSkew = (float) (dx / width);
                trackPosUpdated = true;
            }

            @Override
            public void mousePressed(MouseEvent e) {
                try {
                    sliderSemaphore.acquire();
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
                userChangingPos = true;
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                posSkew = getValue() / 100.0f;
                trackPosUpdated = true;
                userChangingPos = false;
                sliderSemaphore.release();
            }

            @Override
            public void mouseEntered(MouseEvent e) {

            }

            @Override
            public void mouseExited(MouseEvent e) {

            }
        });
    }

    public void startControlFlow() {
        isRunning = true;
        controlThread = new Thread(this::run);
        controlThread.start();
    }

    public void stopControlFlow() {
        if (controlThread != null) {
            isRunning = false;
            try {
                controlThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            controlThread = null;
        }
    }

    @Override
    public void run() {
        while (isRunning) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            float trackLength = Player.getTrackLength(playerHandle);
            float trackPos = Player.getTrackPos(playerHandle);
            while (trackPos < trackLength && isRunning) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                trackPos = Player.getTrackPos(playerHandle);
                if (trackPosUpdated && !userChangingPos) {
                    Player.seekTrack(playerHandle, posSkew);
                    trackPosUpdated = false;
                } else if (!userChangingPos) {
                    try {
                        sliderSemaphore.acquire();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    setValue((int) (trackPos / trackLength * 100.0f));
                    invalidate();
                    sliderSemaphore.release();
                }
            }
        }
    }
}
