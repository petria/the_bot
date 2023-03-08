import {NotificationManager} from 'react-notifications';

const showErrorNotify = (msg, status) => {
    NotificationManager.error(status + ": " + msg, null, 5000, null);


}

const NotifyService = {
    showErrorNotify
};

export default NotifyService;

