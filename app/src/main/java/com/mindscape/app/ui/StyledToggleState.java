package com.mindscape.app.ui;

/**
 * Состояние кастомного тоггла (createStyledSwitch).
 * Хранится в {@code view.getTag()} и используется вызывающим кодом
 * для чтения/изменения состояния и подписки на изменения.
 *
 * Источник: вынесено из внутреннего класса MainActivity.java.
 */
public final class StyledToggleState {

    public interface Listener {
        void onChanged(boolean checked);
    }

    private boolean checked;
    private Listener visualListener;
    private Listener changeListener;

    public StyledToggleState(boolean checked) {
        this.checked = checked;
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        if (this.checked == checked) {
            return;
        }
        this.checked = checked;
        if (visualListener != null) {
            visualListener.onChanged(checked);
        }
        if (changeListener != null) {
            changeListener.onChanged(checked);
        }
    }

    public void setCheckedSilently(boolean checked) {
        if (this.checked == checked) {
            return;
        }
        this.checked = checked;
        if (visualListener != null) {
            visualListener.onChanged(checked);
        }
    }

    public void setVisualListener(Listener listener) {
        this.visualListener = listener;
    }

    public void setOnCheckedChangeListener(Listener listener) {
        this.changeListener = listener;
    }
}
