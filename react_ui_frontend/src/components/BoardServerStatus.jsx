import React, {useEffect, useState} from "react";
import ServerStatusService from "../services/server-status-service";
//import NotifyService  from "../services/notify.service";
import EventBus from "../common/EventBus";

import {NotificationContainer, NotificationManager} from 'react-notifications';

//const data = ServerStatusService.getServerStatus();

const BoardServerStatus = () => {

    const [data, setData] = useState(null);

    useEffect(() => {

        ServerStatusService.getServerStatus(
            (response) => {
                console.log(">> response: ", response);
                setData(response.data);
            },
            (error) => {
                console.log(">> error.response.status: ", error.response.status);
                console.log(">> error: ", error);
                setData(null);
                if (error.response && error.response.status === 401) {
                    EventBus.dispatch("logout");
                }
            }
        )

/*        ServerStatusService.getServerStatus().then(
            (response) => {
                console.log("got response:", response)
                setData(response.data);
            },
            (error) => {
                console.log("Error-> ", error);
                const msg = error.response;
                    (error.response && error.response.data && error.response.data.message) || error.message ||  error.toString();

                NotifyService.showErrorNotify(msg);

                if (error.response && error.response.status === 401) {
                    EventBus.dispatch("logout");
                }

            }
        );
*/

        //setData(ServerStatusService.getServerStatus());
    }, []);
//    console.log('data -->', data);

    const createNotification = (type, msg) => {
        console.log('>>1 createNotification: ' + type);
        switch (type) {
            case 'info':
                NotificationManager.info('Info message');
                break;
            case 'success':
                NotificationManager.success('Success message', 'Title here');
                break;
            case 'warning':
                NotificationManager.warning('Warning message', 'Close after 3000ms', 3000);
                break;
            case 'error':
                console.log('<>>fffufufuuff');
                NotificationManager.error(msg, 'Click me!', 5000, () => {
                    alert('callback');
                });
                break;
            default:
                console.log('<>>default');
        }

    };

    const handleStart = (server) => {
        console.log("start server: ", server);
        createNotification('error')
        return undefined;
    }

    return (
        <>
            TEST
            <table id="example" className="table table-striped table-bordered dt-responsive nowrap">
                <thead>
                <tr>
                    <th>Id</th>
                    <th>Name</th>
                    <th>Type</th>
                    <th>Status</th>
                    <th>Actions</th>
                </tr>
                </thead>
                <tbody>
                {data &&
                    data.servers.map((server, index) =>
                        <tr key={index}>
                            <td>{server.id}</td>
                            <td>{server.name}</td>
                            <td>T:{server.type}</td>
                            <td>{server.status}</td>
                            <td>
                                <button onClick={() => handleStart(server)}>START</button>
                                <button>STOP</button>
                            </td>
                        </tr>
                    )}
                </tbody>
            </table>
            <NotificationContainer/>
        </>
    )
}

export default BoardServerStatus;