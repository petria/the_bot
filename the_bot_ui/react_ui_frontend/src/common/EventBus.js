const eventBus = {
    on(event, callback) {
        console.log("-> eventBus.on", event);
        document.addEventListener(event, (e) => callback(e.detail));
    },
    dispatch(event, data) {
        console.log("-> eventBus.dispatch", event);
        document.dispatchEvent(new CustomEvent(event, {detail: data}));
    },
    remove(event, callback) {
        console.log("-> eventBus.remove", event);
        document.removeEventListener(event, callback);
    },
};

export default eventBus;
