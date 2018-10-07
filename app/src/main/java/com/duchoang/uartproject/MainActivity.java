package com.duchoang.uartproject;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.GpioCallback;
import android.util.Log;
import com.google.android.things.pio.PeripheralManager;
import com.google.android.things.pio.UartDevice;
import com.google.android.things.pio.UartDeviceCallback;
import com.google.android.things.pio.Pwm;
import com.leinardi.android.things.pio.SoftPwm;
import java.io.IOException;

public class MainActivity extends Activity {
    private static int INTERVAL_BETWEEN_BLINKS_MS = 1000;
    private static final String TAG = "MainActivity";
    private String receivedString = "2";

    // Stuffs for UART
    private Handler mHandlerUart = new Handler();
    private Handler mHandlerBlinkLed = new Handler();
    private Handler mHandlerCheckString = new Handler();
    private Handler mHandlerEx5 = new Handler();
    private Handler mHandlerPWM = new Handler();
    private Handler mHandlerPWMEx4 = new Handler();

    private UartDevice mUartDevice;

    // Stuffs for option 1
    private Gpio mLedGpioGreen;
    private Gpio mLedGpioRed;
    private Gpio mLedGpioBlue;
    private int stateOp1;
    private Gpio mButtonGpio;
    private int buttonState = 2;
    private int statePWM = 2;

    private boolean optionOSelected = false;
    private boolean option1Selected = false;
    private boolean option2Selected = false;
    private boolean option3Selected = false;
    private boolean option4Selected = false;
    private boolean option5Selected = false;
    private boolean optionFSelected = false;

    private int ledCounter = 0;
    private int stateEx5 = 0;

    // PWM port initialization
    private static final double MIN_DUTY_CYCLE = 0;
    private static final double MAX_DUTY_CYCLE = 100;
    private static final double DUTY_CYCLE_CHANGE_PER_STEP = 0.1;
    private static final int STEP = 1;
    private double dutyCycle;
    private boolean isIncreasing = true;
    private Pwm mPwmRed;
    private Pwm mPwmGreen;
    private Pwm mPwmBlue;

    private Gpio mButtonPWM;

    // Main function
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            PeripheralManager manager = PeripheralManager.getInstance();
            mUartDevice = manager.openUartDevice("UART0");
            configureUartFrame(mUartDevice);
        }
        catch (IOException e) {
            Log.w(TAG, "Error on PeripheralIO API", e);
        }
        mHandlerUart.post(mUartRunnable);

        // A runnable for toggling between states
        mHandlerCheckString.post(mCheckReceivedStringRunnable);
    }

    // A runnable for UART
    private Runnable mUartRunnable = new Runnable() {
        @Override
        public void run() {
            if (mUartDevice == null) return;
            try {
                mUartDevice.registerUartDeviceCallback(mUartDeviceCallback);
                // sendUartData();
                //mHandlerUart.postDelayed(mUartRunnable, 200);
            } catch (IOException e) {
                Log.e(TAG, "Error on reading from UART ports", e);
            }
        }
    };

    // A runnable for checking received string
    private Runnable mCheckReceivedStringRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                switch (receivedString) {
                    case "O": {
                        if (!optionOSelected) {
                            sendUartData("Ready to receive commands!");
                            optionOSelected = true;
                        }
                        break;
                    }
                    case "1":
                        if (!option1Selected) {
                            mServiceOption1();
                            sendUartData("Selected Option 1! ");
                            option1Selected = true;
                        }
                        break;
                    case "2":
                        if (!option2Selected) {
                            mServiceOption2();
                            sendUartData("Selected Option 2! ");
                            option2Selected = true;

                        }
                        break;
                    case "3":
                        if (!option3Selected) {
                            mServiceOption3();
                            option3Selected = true;
                            sendUartData("Selected Option 3! ");
                        }
                        break;
                    case "4":
                        if (!option4Selected) {
                            mServiceOption4();
                            sendUartData("Selected Option 4! ");
                            option4Selected = true;
                        }
                    case "5":
                        if (!option5Selected) {
                            mServiceOption5();
                            sendUartData("Selected Option 5! ");
                            option5Selected = true;
                        }
                        break;
                    case "F":
                        if (!optionFSelected) {
                            mServiceOptionF();
                            sendUartData("Stop all services! ");
                            optionFSelected = true;
                        }
                        break;
                    default:
                        Log.w(TAG,"Got into default!");
                        break;
                }
            } catch (NullPointerException e) {
                Log.w(TAG, "Toggling between states failed!");
            }
            mHandlerCheckString.postDelayed(this, 500);
        }
    };

    public void mServiceOption1 () {
        // Removing all services
        mHandlerEx5.removeCallbacks(mRunnableEx5);
        mHandlerPWMEx4.removeCallbacks(changePWMREx4);
        if (mButtonPWM != null) {
            mButtonPWM.unregisterGpioCallback(mGpioCallbackPWM);
        }
        mHandlerPWM.removeCallbacks(changePWMRunnable);
        mHandlerBlinkLed.removeCallbacks(mBlinkRunnable);
        if (mButtonGpio != null) {
            mButtonGpio.unregisterGpioCallback(mGpioCallback);

        }

        try {
            if (mPwmGreen!= null && mPwmBlue != null && mPwmRed != null) {
                mPwmBlue.close();
                mPwmRed.close();
                mPwmGreen.close();
            }
            mPwmGreen = null;
            mPwmBlue = null;
            mPwmRed = null;
            mLedGpioRed = PeripheralManager.getInstance().openGpio("BCM2");
            mLedGpioGreen = PeripheralManager.getInstance().openGpio("BCM3");
            mLedGpioBlue = PeripheralManager.getInstance().openGpio("BCM4");
        } catch (Exception e) {
            Log.w(TAG, "Unable to close PWM ports! ");
        }

        // Init ports
        try {
            //
            mLedGpioRed.setDirection(Gpio.DIRECTION_OUT_INITIALLY_HIGH);
            mLedGpioGreen.setDirection(Gpio.DIRECTION_OUT_INITIALLY_HIGH);
            mLedGpioBlue.setDirection(Gpio.DIRECTION_OUT_INITIALLY_HIGH);
            //
            mLedGpioRed.setActiveType(Gpio.ACTIVE_LOW);
            mLedGpioGreen.setActiveType(Gpio.ACTIVE_LOW);
            mLedGpioBlue.setActiveType(Gpio.ACTIVE_LOW);
            //
            stateOp1 = 1;
        } catch (Exception e) {
            Log.w(TAG, "Unable to open GPIO ports! ");
        }

        // Post the main services
        mHandlerBlinkLed.post(mBlinkRunnable);
    }

    public void mServiceOption2 () {

        // Removing all services
        mHandlerEx5.removeCallbacks(mRunnableEx5);
        mHandlerPWMEx4.removeCallbacks(changePWMREx4);
        if (mButtonPWM != null) {
            mButtonPWM.unregisterGpioCallback(mGpioCallbackPWM);
        }
        mHandlerPWM.removeCallbacks(changePWMRunnable);
        mHandlerBlinkLed.removeCallbacks(mBlinkRunnable);
        if (mButtonGpio != null) {
            mButtonGpio.unregisterGpioCallback(mGpioCallback);

        }

        // Close ports
        try {
            if (mPwmGreen!= null && mPwmBlue != null && mPwmRed != null && mButtonPWM != null) {
                mPwmBlue.close();
                mPwmRed.close();
                mPwmGreen.close();
                mButtonPWM.close();
            }

//            mButtonPWM = null;
//            mPwmGreen = null;
//            mPwmBlue = null;
//            mPwmRed = null;

            mLedGpioRed = PeripheralManager.getInstance().openGpio("BCM2");
            mLedGpioGreen = PeripheralManager.getInstance().openGpio("BCM3");
            mLedGpioBlue = PeripheralManager.getInstance().openGpio("BCM4");
            mButtonGpio = PeripheralManager.getInstance().openGpio("BCM21");

        } catch (Exception e) {
            Log.w(TAG, "Unable to close PWM ports! ");
        }

        // Init ports here
        try {

            mLedGpioRed.setDirection(Gpio.DIRECTION_OUT_INITIALLY_HIGH);
            mLedGpioGreen.setDirection(Gpio.DIRECTION_OUT_INITIALLY_HIGH);
            mLedGpioBlue.setDirection(Gpio.DIRECTION_OUT_INITIALLY_HIGH);
            mButtonGpio.setDirection(Gpio.DIRECTION_IN);
            mButtonGpio.setEdgeTriggerType(Gpio.EDGE_FALLING);
            //
            mLedGpioRed.setActiveType(Gpio.ACTIVE_LOW);
            mLedGpioGreen.setActiveType(Gpio.ACTIVE_LOW);
            mLedGpioBlue.setActiveType(Gpio.ACTIVE_LOW);
            mButtonGpio.setActiveType(Gpio.ACTIVE_HIGH);
            //
            buttonState = 2;
            stateOp1 = 1;
        } catch (Exception e) {
            Log.w(TAG, "Hey, there comes an error!!!");
        }

        // Post the main services
        mHandlerBlinkLed.post(mBlinkRunnable);
        try {
            Log.w(TAG, "Get into GPIO try of SV2!");
            mButtonGpio.registerGpioCallback(mGpioCallback);
        } catch (Exception e) {
            Log.w(TAG, "Cannot register button (Option 2)!");
        }
    }

    public void mServiceOption3 () {
        // Removing all services
        mHandlerEx5.removeCallbacks(mRunnableEx5);
        mHandlerPWMEx4.removeCallbacks(changePWMREx4);
        if (mButtonPWM != null) {
            mButtonPWM.unregisterGpioCallback(mGpioCallbackPWM);
        }
        mHandlerPWM.removeCallbacks(changePWMRunnable);
        mHandlerBlinkLed.removeCallbacks(mBlinkRunnable);
        if (mButtonGpio != null) {
            mButtonGpio.unregisterGpioCallback(mGpioCallback);

        }

        try {
            if (mLedGpioGreen!= null && mLedGpioRed != null && mLedGpioBlue != null) {
                mLedGpioRed.close();
                mLedGpioBlue.close();
                mLedGpioGreen.close();
            }

//            mLedGpioGreen = null;
//            mLedGpioRed = null;
//            mLedGpioBlue = null;

            mPwmRed = SoftPwm.openSoftPwm("BCM2");
            mPwmGreen = SoftPwm.openSoftPwm("BCM3");
            mPwmBlue = SoftPwm.openSoftPwm("BCM4");

            initializePwm(mPwmRed);
            initializePwm(mPwmGreen);
            initializePwm(mPwmBlue);
        } catch (Exception e) {
            Log.w(TAG, "Unable to close PWM ports (Option 3)! ");
        }


        // Post the main services
        mHandlerPWM.post(changePWMRunnable);
    }

    public void mServiceOption4 () {
        // Removing all services
        mHandlerEx5.removeCallbacks(mRunnableEx5);
        mHandlerPWMEx4.removeCallbacks(changePWMREx4);
        if (mButtonPWM != null) {
            mButtonPWM.unregisterGpioCallback(mGpioCallbackPWM);
        }
        mHandlerPWM.removeCallbacks(changePWMRunnable);
        mHandlerBlinkLed.removeCallbacks(mBlinkRunnable);
        if (mButtonGpio != null) {
            mButtonGpio.unregisterGpioCallback(mGpioCallback);
        }
        try {
            if (mLedGpioGreen!= null && mLedGpioRed != null && mLedGpioBlue != null && mButtonGpio != null) {
                mLedGpioRed.close();
                mLedGpioBlue.close();
                mLedGpioGreen.close();
                mButtonGpio.close();
            }

            mPwmRed = SoftPwm.openSoftPwm("BCM2");
            mPwmGreen = SoftPwm.openSoftPwm("BCM3");
            mPwmBlue = SoftPwm.openSoftPwm("BCM4");

            initializePwm(mPwmRed);
            initializePwm(mPwmGreen);
            initializePwm(mPwmBlue);

            mButtonPWM = PeripheralManager.getInstance().openGpio("BCM21");
            mButtonPWM.setDirection(Gpio.DIRECTION_IN);
            mButtonPWM.setEdgeTriggerType(Gpio.EDGE_FALLING);
            mButtonPWM.setActiveType(Gpio.ACTIVE_HIGH);
        } catch (Exception e) {
            Log.w(TAG, "Unable to close PWM ports (Option 4)! ");
        }
        // Post the main services
        try {
            mButtonPWM.registerGpioCallback(mGpioCallbackPWM);
        } catch (Exception e) {
            Log.w(TAG, "Error on opening button PWM (Option 4)!");
        }
        mHandlerPWMEx4.post(changePWMREx4);
    }

    public void mServiceOption5 () {
        // Removing all services
        mHandlerEx5.removeCallbacks(mRunnableEx5);
        mHandlerPWMEx4.removeCallbacks(changePWMREx4);
        if (mButtonPWM != null) {
            mButtonPWM.unregisterGpioCallback(mGpioCallbackPWM);
        }
        mHandlerPWM.removeCallbacks(changePWMRunnable);
        mHandlerBlinkLed.removeCallbacks(mBlinkRunnable);
        if (mButtonGpio != null) {
            mButtonGpio.unregisterGpioCallback(mGpioCallback);

        }
        try {
            if (mPwmGreen!= null && mPwmBlue != null && mPwmRed != null) {
                mPwmBlue.close();
                mPwmRed.close();
                mPwmGreen.close();
            }

            mLedGpioRed = PeripheralManager.getInstance().openGpio("BCM2");
            mLedGpioGreen = PeripheralManager.getInstance().openGpio("BCM3");
            mLedGpioBlue = PeripheralManager.getInstance().openGpio("BCM4");
        } catch (Exception e) {
            Log.w(TAG, "Unable to close PWM ports (Option 5)! ");
        }

        // Init ports
        try {
            // Define them as outputs
            mLedGpioRed.setDirection(Gpio.DIRECTION_OUT_INITIALLY_HIGH);
            mLedGpioGreen.setDirection(Gpio.DIRECTION_OUT_INITIALLY_HIGH);
            mLedGpioBlue.setDirection(Gpio.DIRECTION_OUT_INITIALLY_HIGH);

            // Assign initial states
            mLedGpioRed.setActiveType(Gpio.ACTIVE_LOW);
            mLedGpioGreen.setActiveType(Gpio.ACTIVE_LOW);
            mLedGpioBlue.setActiveType(Gpio.ACTIVE_LOW);

            stateEx5 = 0;
        } catch (IOException e) {
            Log.w(TAG, "Unable to open GPIO (Option 5)", e);
        }

        // Post the main services
        mHandlerEx5.post(mRunnableEx5);
    }

    public void mServiceOptionF () {
        mHandlerEx5.removeCallbacks(mRunnableEx5);
        mHandlerPWMEx4.removeCallbacks(changePWMREx4);
        if (mButtonPWM != null) {
            mButtonPWM.unregisterGpioCallback(mGpioCallbackPWM);
        }
        mHandlerPWM.removeCallbacks(changePWMRunnable);
        mHandlerBlinkLed.removeCallbacks(mBlinkRunnable);
        if (mButtonGpio != null) {
            mButtonGpio.unregisterGpioCallback(mGpioCallback);

        }
        try {
            if (mPwmBlue != null ||  mPwmRed != null || mPwmGreen != null || mButtonPWM != null || mButtonGpio != null || mLedGpioBlue != null || mLedGpioGreen != null || mLedGpioRed != null) {
                mButtonPWM.close();
                mPwmGreen.close();
                mPwmBlue.close();
                mPwmRed.close();
                mButtonGpio.close();
                mLedGpioGreen.close();
                mLedGpioRed.close();
                mLedGpioBlue.close();
            }
        } catch (Exception e) {
            Log.w(TAG, "Unable to close PWM ports (Option 5)! ");
        }
    }


    private GpioCallback mGpioCallbackPWM = new GpioCallback() {
        @Override
        public boolean onGpioEdge(Gpio button) {
            try {
                switch (statePWM) {
                    case 1:
                        if (!button.getValue()) {
                            statePWM = 2;
                        }
                        break;
                    case 2:
                        if (!button.getValue()) {
                            statePWM = 3;
                        }
                        break;
                    case 3:
                        if (!button.getValue()) {
                            statePWM = 4;
                        }
                        break;
                    case 4:
                        if (!button.getValue()) {
                            statePWM = 1;
                        }
                        break;
                    default:
                        break;

                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
            }
            return true;
        }
    };

    private Runnable changePWMREx4 = new Runnable() {
        @Override
        public void run() {
            if (mPwmRed == null || mPwmBlue == null || mPwmGreen == null) {
                Log.w(TAG, "Stopping runnable since mPwm is null");
                return;
            }

            if (isIncreasing) {
                dutyCycle += DUTY_CYCLE_CHANGE_PER_STEP;
            } else {
                dutyCycle -= DUTY_CYCLE_CHANGE_PER_STEP;
            }

            if (dutyCycle > MAX_DUTY_CYCLE) {
                dutyCycle = MAX_DUTY_CYCLE;
                isIncreasing = !isIncreasing;
            } else if (dutyCycle < MIN_DUTY_CYCLE) {
                dutyCycle = MIN_DUTY_CYCLE;
                isIncreasing = !isIncreasing;
            }

            Log.d(TAG, "Changing PWM duty cycle to" + dutyCycle);

            try {
                switch (statePWM) {
                    // Use button inputs to change states of LEDs
                    case 1:
                        mPwmRed.setPwmDutyCycle(dutyCycle);
                        mPwmGreen.setPwmDutyCycle(dutyCycle);
                        mPwmBlue.setPwmDutyCycle(dutyCycle);
                        break;
                    case 2:
                        mPwmRed.setPwmDutyCycle(dutyCycle);
                        mPwmGreen.setPwmDutyCycle(0);
                        mPwmBlue.setPwmDutyCycle(0);
                        break;
                    case 3:
                        mPwmRed.setPwmDutyCycle(0);
                        mPwmGreen.setPwmDutyCycle(dutyCycle);
                        mPwmBlue.setPwmDutyCycle(0);
                        break;
                    case 4:
                        mPwmRed.setPwmDutyCycle(0);
                        mPwmGreen.setPwmDutyCycle(0);
                        mPwmBlue.setPwmDutyCycle(dutyCycle);
                        break;
                    default:
                        break;
                }

                mHandlerPWMEx4.postDelayed(this, STEP);
            } catch (IOException e) {
                Log.e(TAG, "Error on PeripheralIO API", e);
            }
        }
    };


    // PWM init
    public void initializePwm(Pwm pwm) throws IOException {
        pwm.setPwmFrequencyHz(200);
        pwm.setPwmDutyCycle(20);

        // Enable the PWM signal
        pwm.setEnabled(true);
    }

    // A runnable for Option 1
    private Runnable mBlinkRunnable = new Runnable() {
        @Override
        public void run() {
            if (mLedGpioBlue == null || mLedGpioGreen == null || mLedGpioRed == null) {
                return;
            }
            try {
                // Toggle between states using a simple finite state machine
                switch (stateOp1) {
                    case 1:
                        // RGB: 100
                        mLedGpioRed.setValue(true);
                        mLedGpioGreen.setValue(false);
                        mLedGpioBlue.setValue(false);
                        stateOp1 = 2;
                        break;
                    case 2:
                        // RGB: 010
                        mLedGpioRed.setValue(false);
                        mLedGpioGreen.setValue(true);
                        mLedGpioBlue.setValue(false);
                        stateOp1 = 3;
                        break;
                    case 3:
                        // RGB: 001
                        mLedGpioRed.setValue(false);
                        mLedGpioGreen.setValue(false);
                        mLedGpioBlue.setValue(true);
                        stateOp1 = 1;
                        break;
                    default:
                        break;
                }

                // Reschedule the same runnable
                mHandlerBlinkLed.postDelayed(mBlinkRunnable, INTERVAL_BETWEEN_BLINKS_MS);

            } catch (IOException e) {
                Log.e(TAG, "Error on PeripheralIO API", e);
            }
        }
    };

    // A runnable for button activity
    private GpioCallback mGpioCallback = new GpioCallback() {
        @Override
        public boolean onGpioEdge(Gpio button) {
            try {
                Log.i(TAG, "Read from BUTTON!!!!!!!!!!!!!!!!!!!!!!");
                // An FSM for toggling between states of buttons as input
                switch (buttonState) {
                    case 1:
                        if (!button.getValue()) {
                            INTERVAL_BETWEEN_BLINKS_MS = 2000;
                            buttonState = 2;
                        }
                        break;
                    case 2:
                        if (!button.getValue()) {
                            INTERVAL_BETWEEN_BLINKS_MS = 1000;
                            buttonState = 3;
                        }
                        break;
                    case 3:
                        if (!button.getValue()) {
                            INTERVAL_BETWEEN_BLINKS_MS = 500;
                            buttonState = 4;
                        }
                        break;
                    case 4:
                        if (!button.getValue()) {
                            INTERVAL_BETWEEN_BLINKS_MS = 100;
                            buttonState = 1;
                        }
                        break;
                    default:
                        break;

                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                Log.i(TAG, "Read from BUTTON!!!!!!!!!!!!!!!!!!!!!!");
            }
            return true;
        }
    };

    // PWM runnable
    private Runnable changePWMRunnable = new Runnable() {
        @Override
        public void run() {
            if (mPwmRed == null || mPwmBlue == null || mPwmGreen == null) {
                Log.w(TAG, "Stopping runnable since mPwm is null");
                return;
            }

            if (isIncreasing) {
                dutyCycle += DUTY_CYCLE_CHANGE_PER_STEP;
            } else {
                dutyCycle -= DUTY_CYCLE_CHANGE_PER_STEP;
            }

            if (dutyCycle > MAX_DUTY_CYCLE) {
                dutyCycle = MAX_DUTY_CYCLE;
                isIncreasing = !isIncreasing;
            } else if (dutyCycle < MIN_DUTY_CYCLE) {
                dutyCycle = MIN_DUTY_CYCLE;
                isIncreasing = !isIncreasing;
            }

            Log.d(TAG, "Changing PWM duty cycle to" + dutyCycle);

            try {

                mPwmRed.setPwmDutyCycle(dutyCycle);
                mPwmGreen.setPwmDutyCycle(dutyCycle);
                mPwmBlue.setPwmDutyCycle(dutyCycle);
                mHandlerPWM.postDelayed(this, STEP);
            } catch (IOException e) {
                Log.e(TAG, "Error on PeripheralIO API", e);
            }
        }
    };

    // Exercise 5
    private Runnable mRunnableEx5 = new Runnable() {
        @Override
        public void run() {
            if (mLedGpioBlue == null || mLedGpioGreen == null || mLedGpioRed == null) {
                return;
            }
            try {
                // Toggle between states
                switch(stateEx5) {
                    case 0:
                        // After any LEDs ON, any states must get back to state of LEDs OFF all.
                    mLedGpioRed.setValue(false);
                    mLedGpioGreen.setValue(false);
                    mLedGpioBlue.setValue(false);
                    ledCounter++;
                    if (ledCounter == 5 || ledCounter == 9) {
                        stateEx5 = 2;
                    }
                    else if (ledCounter == 7) {
                        stateEx5 = 4;
                    }
                    else if (ledCounter == 13) {
                        stateEx5 = 3;
                    }
                    else {
                        stateEx5 = 1;
                    }
                    break;
                    case 1:
                        // RGB: 100
                        mLedGpioRed.setValue(true);
                        mLedGpioGreen.setValue(false);
                        mLedGpioBlue.setValue(false);
                        stateEx5 = 0;
                        break;
                    case 2:
                        // RGB: 110
                        mLedGpioRed.setValue(true);
                        mLedGpioGreen.setValue(true);
                        mLedGpioBlue.setValue(false);
                        stateEx5 = 0;
                        break;
                    case 3:
                        // RGB: 111
                        mLedGpioRed.setValue(true);
                        mLedGpioGreen.setValue(true);
                        mLedGpioBlue.setValue(true);
                        stateEx5 = 0;
                        ledCounter = 0;
                        break;
                    case 4:
                        // RGB: 101
                        mLedGpioRed.setValue(true);
                        mLedGpioGreen.setValue(false);
                        mLedGpioBlue.setValue(true);
                        stateEx5 = 0;
                        break;
                    default:
                        break;
                }

                // 250 mil for the smallest pace as of RED LED (0.5s)
                mHandlerEx5.postDelayed(mRunnableEx5, 250);

            } catch (IOException e) {
                Log.e(TAG, "Error on PeripheralIO API", e);
            }
        }
    };

    // Destroy everything when done
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mUartDevice != null) {
            try {
                mUartDevice.close();
                mLedGpioGreen.close();
                mLedGpioBlue.close();
                mLedGpioRed.close();
                mButtonPWM.close();
                mPwmGreen.close();
                mPwmBlue.close();
                mPwmRed.close();
            } catch (IOException e) {
                Log.w(TAG, "Cannot close UART device ", e);
            } finally {
                mUartDevice = null;
                mLedGpioRed = null;
                mLedGpioBlue = null;
                mLedGpioGreen = null;
                mPwmRed = null;
                mPwmBlue = null;
                mPwmGreen = null;
                mButtonPWM = null;
            }
        }
    }

    // Configure UART frame
    public void configureUartFrame(UartDevice uart) throws IOException {
        uart.setBaudrate(115200);
        uart.setDataSize(8);
        uart.setParity(0);
        uart.setStopBits(1);
    }

    // When data available, receive them
    private UartDeviceCallback mUartDeviceCallback = new UartDeviceCallback() {
        @Override
        public boolean onUartDeviceDataAvailable(UartDevice uartDevice) {
            try {
                receiveUartData(uartDevice);
            } catch (Exception e){

            }
            return true;
        }
    };

    // Receive data
    public void receiveUartData(UartDevice uartDevice) {
        byte[] buffer = new byte[512];
        int counter;
        try {
            while ((counter = uartDevice.read(buffer, buffer.length)) > 0) {

                String str = new String(buffer);
                receivedString = str.substring(0, 1);
                switch (receivedString) {
                    case "O":
                        optionOSelected = false;
                        break;
                    case "1":
                        option1Selected = false;
                        break;
                    case "2":
                        option2Selected = false;
                        break;
                    case "3":
                        option3Selected = false;
                        break;
                    case "4":
                        option4Selected = false;
                        break;
                    case "5":
                        option5Selected = false;
                        break;
                    case "F":
                        optionFSelected = false;
                        break;
                    default:
                        optionFSelected = false;
                        break;
                }
                Log.i(TAG, "Read from UART ports: " + receivedString);
        }
        } catch (IOException e) {
            Log.w(TAG, "Error while receiving data via UART ports.");
        }
    }

    // Send any string to targeted device
    public void sendUartData(String str) {
        if (mUartDevice == null) return;
        try {
            byte[] buffer = new byte[100];
            buffer = str.getBytes();
            mUartDevice.write(buffer, buffer.length);
        } catch (IOException e) {
            Log.w(TAG, "Error while sending data via UART ports.");
        }
    }
}
