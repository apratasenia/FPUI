/// <reference path="/App/app.js" />

// The initialize function must be run each time a new page is loaded.
(function () {
    Office.initialize = function (reason) {
        App.initialize();
        console.log("Office.initialize on " + new Date());
        console.log("Add-in host URL " + window.location.origin);
    };
})();

function onLoadVersionDataClick(event) {
    console.log("onLoadVersionDataClick");
    App.loadData()
        .then(function () {
            event.completed();
        })
        .catch(function (error) {
            event.completed();
            console.log("Error: " + error);
            if (error instanceof OfficeExtension.Error) {
                console.log("Debug info: " + JSON.stringify(error.debugInfo));
            }
        });
}

function onSaveVersionDataClick(event) {
    console.log("onSaveVersionDataClick");
    App.saveData()
        .then(function () {
            event.completed();
        })
        .catch(function (error) {
            event.completed();
            console.log("Error: " + error);
            if (error instanceof OfficeExtension.Error) {
                console.log("Debug info: " + JSON.stringify(error.debugInfo));
            }
        });
}
