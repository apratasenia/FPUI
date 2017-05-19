var __extends = (this && this.__extends) || function (d, b) {
    for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p];
    function __() { this.constructor = d; }
    d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
};
var Tables;
(function (Tables) {
    var Table = (function () {
        function Table(tableName, fields) {
            this.entireRows = true;
            this.rowCount = null;
            // Event handler to refresh the table when the
            // data in the table changes.
            this.onBindingDataChanged = function (result) {
            };
            this.tableName = tableName;
            this.fields = fields;
            this.initializeBinding();
        }
        Table.prototype.table = function (ctx) {
            return;
        };
        Table.prototype.jsonToArray = function (json) {
            return json.map(function (row) { return this.fields.map(function (field) { return row[field]; }); }, this);
        };
        Table.prototype.arrayToJson = function (arr, json) {
            arr.forEach(function (line, lineIdx) {
                this.fields.forEach(function (field, fieldIdx) {
                    if (json[lineIdx].hasOwnProperty(field)) {
                        json[lineIdx][field] = line[fieldIdx];
                    }
                });
            }, this);
        };
        Table.prototype.fromJSON = function (json) {
            console.log("fromJSON : " + JSON.stringify(json));
            return this.fromArray(this.jsonToArray(json));
        };
        Table.prototype.toJSON = function (json) {
            var self = this;
            return Excel.run(function (ctx) {
                var data = self.getDataRange(ctx);
                data.load("values");
                return ctx.sync().
                    then(function () {
                    self.arrayToJson(data.values, json);
                });
            });
        };
        Table.prototype.getDataRange = function (ctx) {
            return ctx.workbook.names.getItem(this.tableName).getRange().getResizedRange(-1, 0); //remove tech footer row
        };
        Table.prototype.fromArray = function (lines) {
            var self = this;
            return Excel.run(function (ctx) {
                var data = self.getDataRange(ctx);
                if (self.rowCount == null) {
                    data.load(['rowCount']);
                    return ctx.sync().then(function () {
                        self.rowCount = data.rowCount;
                        self.updateExcel(ctx, data, lines);
                        return ctx.sync();
                    });
                }
                else {
                    self.updateExcel(ctx, data, lines);
                    return ctx.sync();
                }
            });
        };
        Table.prototype.updateExcel = function (ctx, data, lines) {
            if (lines.length > this.rowCount) {
                var targetRange = this.entireRows ? data.getRowsBelow(lines.length - this.rowCount).getEntireRow() : data.getRowsBelow(lines.length - this.rowCount);
                targetRange.insert(Excel.InsertShiftDirection.down);
            }
            else if (lines.length < this.rowCount) {
                var targetRange = this.entireRows ? data.getResizedRange(-lines.length, 0).getEntireRow() : data.getResizedRange(-lines.length, 0);
                targetRange.delete(Excel.DeleteShiftDirection.up);
            }
            this.getDataRange(ctx).values = lines;
            this.rowCount = lines.length;
        };
        Table.prototype.initializeBinding = function () {
            var self = this;
            Office.context.document.bindings.addFromNamedItemAsync(this.tableName, Office.BindingType.Table, { id: this.tableName }, function (results) {
                self.binding = results.value;
                //this.addBindingsHandler();
            });
        };
        // Add the handler to the BindingDataChanged event of the binding.
        // When the event is raised, the callback function is called.
        Table.prototype.addBindingsHandler = function (callback) {
            this.binding.addHandlerAsync(Office.EventType.BindingDataChanged, this.onBindingDataChanged, function (results) { (results.status == Office.AsyncResultStatus.Succeeded && callback && callback()); });
        };
        Table.prototype.removeBindingsHandler = function (callback) {
            this.binding.removeHandlerAsync(Office.EventType.BindingDataChanged, { handler: this.onBindingDataChanged }, function (results) { (results.status == Office.AsyncResultStatus.Succeeded && callback && callback()); });
        };
        Table.prototype.toString = function () {
            return this.tableName;
        };
        return Table;
    }());
    var TableResources = (function (_super) {
        __extends(TableResources, _super);
        function TableResources() {
            _super.call(this, "tblResourcesData", ["Chk", "RESOURCE_NAME", "SORT_GROUP_ID", "RESOURCE_TYPE_ID", "USER_ID", "COMP_CODE", "SLID", "THIRD_PARTY_NAME", "EXPERIENCE_LEVEL", "COST_CURRENCY_TC", "COST_RATE_TC", "COST_RATE_PC", "INFLATION_RATE"]);
        }
        return TableResources;
    }(Table));
    Tables.TableResources = TableResources;
})(Tables || (Tables = {}));
/// <reference path="cpm_tables.ts" />
//import {Tables} from "./cpm_tables";
var App;
(function (App) {
    App.tblResources = null;
    var initialized = false;
    var planId;
    var planNo;
    var planData = null; //JSON
    // Common initialization function (to be called from each page)
    function initialize() {
        console.log(Office.context.document.url);
        planId = param('planId');
        planNo = param('planNo');
        App.tblResources = new Tables.TableResources();
        initialized = true;
    }
    App.initialize = initialize;
    function loadData() {
        return Promisify.jQuery($.getJSON(dataUrl())).then(function (data) {
            console.log('Plan data loaded');
            Office.context.document.settings.set("planData", data);
            return Promisify.async(Office.context.document.settings.saveAsync).then(function () {
                console.log('Plan data stored in Settings');
                return App.tblResources.fromJSON(data["ITAB"]).then(function () {
                    console.log('Plan data loaded into Excel');
                });
            });
        });
    }
    App.loadData = loadData;
    function saveData() {
        var data = Office.context.document.settings.get("planData");
        return App.tblResources.toJSON(data["ITAB"]).then(function () {
            console.log('Plan data composed');
            Office.context.document.settings.set("planData", data);
            return Promisify.async(Office.context.document.settings.saveAsync).then(function () {
                console.log('Plan data stored in Settings');
                return Promisify.jQuery($.post(dataUrl(), data, 'json')).then(function () {
                    console.log('Plan data saved to Backend');
                });
            });
        });
    }
    App.saveData = saveData;
    function showNotification(header, text) {
        if (!initialized) {
            console.log('Add-in has not yet been initialized.');
            return;
        }
        var param = encodeURIComponent(text);
        Promisify.dialog(Office.context.ui.displayDialogAsync, url('/dialogs/alert.html?' + param)).then(function (arg) {
            console.log(arg.message);
        });
    }
    App.showNotification = showNotification;
    function param(name) {
        return (Office.context.document.url.split(name + '=')[1] || '').split('&')[0];
    }
    function dataUrl() {
        return this.url("/sap/bc/cpmrest/" + planId + '/' + planNo);
    }
    function url(path) {
        if (path === void 0) { path = ''; }
        return location.origin + path;
    }
})(App || (App = {}));
var Promisify;
(function (Promisify) {
    function jQuery(fn) {
        return new OfficeExtension.Promise(function (resolve, reject) {
            fn.done(function (data) { resolve(data); }).fail(function (jqXHR, textStatus, errorThrown) { reject(textStatus); });
        });
    }
    Promisify.jQuery = jQuery;
    function async(fn) {
        return new OfficeExtension.Promise(function (resolve, reject) {
            fn(function (result) {
                if (result.status == Office.AsyncResultStatus.Succeeded)
                    resolve(result);
                else
                    reject(result);
            });
        });
    }
    Promisify.async = async;
    ;
    function dialog(fn, startAddress, options) {
        return new OfficeExtension.Promise(function (resolve, reject) {
            fn(startAddress, { height: 30, width: 20 }, function (result) {
                if (result.status == Office.AsyncResultStatus.Succeeded)
                    resolve(result);
                else
                    reject(result);
            });
        }).then(function (result) {
            var dialog = result.value;
            return new OfficeExtension.Promise(function (resolve, reject) {
                dialog.addEventHandler(Office.EventType.DialogMessageReceived, function (arg) {
                    dialog.close();
                    if (arg.error)
                        reject(arg.error);
                    else
                        resolve(arg.message);
                });
            });
        });
    }
    Promisify.dialog = dialog;
    ;
})(Promisify || (Promisify = {}));
//# sourceMappingURL=bundle.js.map