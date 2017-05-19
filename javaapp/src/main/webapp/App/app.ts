/// <reference path="cpm_tables.ts" />

//import {Tables} from "./cpm_tables";

namespace App {

    export let tblResources: Tables.TableResources = null;

    let initialized: boolean = false;
    let planId: string;
    let planNo: string;
    let planData: any = null;   //JSON

    // Common initialization function (to be called from each page)
    export function initialize() {
        console.log(Office.context.document.url);
        planId = param('planId');
        planNo = param('planNo');
        tblResources = new Tables.TableResources();
        initialized = true;
    }

    export function loadData(): OfficeExtension.IPromise<void> {
        return Promisify.jQuery($.getJSON(dataUrl())).then(function (data) {
            console.log('Plan data loaded');
            Office.context.document.settings.set("planData", data);
            return Promisify.async(Office.context.document.settings.saveAsync).then(function () {
                console.log('Plan data stored in Settings');
                return tblResources.fromJSON(data["ITAB"]).then(function () {
                    console.log('Plan data loaded into Excel');
                });
            });
        });
    }

    export function saveData(): OfficeExtension.IPromise<void> {
        let data = Office.context.document.settings.get("planData");
        return tblResources.toJSON(data["ITAB"]).then(function () {
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

    export function showNotification(header: string, text: string) {
        if (!initialized) {
            console.log('Add-in has not yet been initialized.');
            return;
        }
        let param = encodeURIComponent(text);
        Promisify.dialog(Office.context.ui.displayDialogAsync, url('/dialogs/alert.html?' + param)).then(function (arg) {
            console.log(arg.message);
        });
    }

    function param(name: string): string {
        return (Office.context.document.url.split(name + '=')[1] || '').split('&')[0];
    }

    function dataUrl(): string {
        return this.url("/sap/bc/cpmrest/" + planId + '/' + planNo);
    }

    function url(path: string = ''): string {
        return location.origin + path;
    }

}

module Promisify {

    export function jQuery(fn: JQueryXHR): OfficeExtension.IPromise<any> {
        return new OfficeExtension.Promise<any>(function (resolve, reject) {
            fn.done(function (data) { resolve(data); }).fail(function (jqXHR, textStatus, errorThrown) { reject(textStatus); });
        });
    }

    export function async(fn: (action?: (result: Office.AsyncResult) => void) => void): OfficeExtension.IPromise<any> {
        return new OfficeExtension.Promise<Office.AsyncResult>(function (resolve, reject) {
            fn(function (result: Office.AsyncResult) {
                if (result.status == Office.AsyncResultStatus.Succeeded) resolve(result); else reject(result);
            });
        });
    };

    export function dialog(fn: (startAddress: string, options?: Office.DialogOptions, callback?: (result: Office.AsyncResult) => void) => void, startAddress: string, options?: Office.DialogOptions): OfficeExtension.IPromise<any> {
        return new OfficeExtension.Promise<Office.AsyncResult>(function (resolve, reject) {
            fn(startAddress, {height: 30, width: 20 }, function (result: Office.AsyncResult) {
                if (result.status == Office.AsyncResultStatus.Succeeded) resolve(result); else reject(result);
            });
        }).then(function (result) {
            let dialog = result.value;
            return new OfficeExtension.Promise<Office.AsyncResult>(function (resolve, reject) {
                dialog.addEventHandler(Office.EventType.DialogMessageReceived, function (arg) {
                    dialog.close();
                    if (arg.error) reject(arg.error); else resolve(arg.message);
                });
            });
        });
    };

}