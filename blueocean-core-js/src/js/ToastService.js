/**
 * Created by cmeyers on 8/18/16.
 */
import { action, observable, computed } from 'mobx';

/**
 * Holds one or more toasts in state for display in UI.
 */
export class ToastService {

    @observable toasts = [];

    /**
     * Creates a new toast that is added to the list.
     *
     * @param toast object with the following shape:
     * {
     *  text: string, message text to display
     *  action: string, text for action link
     *  onActionClick: function, callback to invoke when action link is clicked
     *  onDismiss: function, callback to invoke when toast is dismissed (immediately, or after timeout)
     *  dismissDelay: number, duration in millis after which to auto-dismiss this Toast
     *  id: unique identifier (optional, will be autogenerated if ommitted)
     * }
     * @returns {number} unique ID of toast
     */
    @action
    newToast(toast) {
        // prevent duplicate toasts from appearing when multiple UI elements
        // are listening for an event that triggers creation of a toast
        if (this._hasDuplicate(toast)) {
            return null;
        }

        const newToast = toast;

        if (!newToast.id) {
            newToast.id = Math.random() * Math.pow(10, 16);
        }

        this.toasts.push(newToast);

        return newToast.id;
    }

    /**
     * Removes a toast with the matching value of toast.id.
     *
     * @param toast
     */
    @action
    removeToast(toast) {
        this.toasts = this.toasts.filter((item) =>
            toast.id !== item.id
        );
    }

    @computed
    get count() {
        return this.toasts ? this.toasts.length : 0;
    }

    /**
     * Returns true if a toast with the same 'text' and 'action' already exists
     * @param newToast
     * @returns {boolean}
     * @private
     */
    _hasDuplicate(newToast) {
        for (const toast of this.toasts) {
            if (toast.text === newToast.text &&
                toast.action === newToast.action) {
                return true;
            }
        }

        return false;
    }

}
