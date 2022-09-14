
const getServerStatus = () => {
    var data = {
        "servers": [
            {
                "id": "1",
                "name": "server1",
                "type": "IRC",
                "status": "online"
            },
            {
                "id": "2",
                "name": "server2",
                "type": "Telegram",
                "status": "offLine"
            }
        ]
    };
    return data;
}

const ServerStatusService = {
    getServerStatus
};

export default ServerStatusService;