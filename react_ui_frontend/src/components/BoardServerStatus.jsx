import React, {useEffect, useState} from "react";
import ServerStatusService from "../services/server-status-service";

//const data = ServerStatusService.getServerStatus();

const BoardServerStatus = () => {

    const [data, setData] = useState(null);

    useEffect(() => {
        setData(ServerStatusService.getServerStatus());
    }, []);
    console.log('data -->', data);

    const handleStart = (server) => {
        console.log("start server: ", server);
        return undefined;
    }

    return (
        <>
            TEST
            <table id="example" className="table table-striped table-bordered dt-responsive nowrap" >
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

        </>
    )
}

export default BoardServerStatus;