module Tables {

    class Table {

        protected tableName: string;
        protected fields: string[];
        private entireRows: boolean = true;
        private rowCount: number = null;
        private binding: any;

        constructor(tableName: string, fields: string[]) {
            this.tableName = tableName;
            this.fields = fields;
            this.initializeBinding();
        }

        private table(ctx: Excel.RequestContext): Excel.Table {
            return;
        }

        private jsonToArray(json: any[]): any[] {
            return json.map(function (row) { return this.fields.map(function (field) { return row[field] }) }, this);
        }

        private arrayToJson(arr: any[], json: any[]) {
            arr.forEach(function (line, lineIdx) {
                this.fields.forEach(function (field, fieldIdx) {
                    if (json[lineIdx].hasOwnProperty(field)) {
                        json[lineIdx][field] = line[fieldIdx];
                    }
                });
            }, this);
        }

        public fromJSON(json: any[]): OfficeExtension.IPromise<{}> {
            console.log("fromJSON : " + JSON.stringify(json));
            return this.fromArray(this.jsonToArray(json));
        }

        public toJSON(json: any[]): OfficeExtension.IPromise<void> {
            let self = this;
            return Excel.run(function (ctx) {
                let data = self.getDataRange(ctx);
                data.load("values");
                return ctx.sync().
                    then(function () {
                        self.arrayToJson(data.values, json);
                    })
            });
        }

        private getDataRange(ctx: Excel.RequestContext): Excel.Range {
            return ctx.workbook.names.getItem(this.tableName).getRange().getResizedRange(-1, 0);    //remove tech footer row
        }

        public fromArray(lines: any[]): OfficeExtension.IPromise<{}> {
            let self = this;
            return Excel.run(function (ctx) {
                let data = self.getDataRange(ctx);
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
        }

        private updateExcel(ctx: Excel.RequestContext, data: Excel.Range, lines: any[]) {
            if (lines.length > this.rowCount) {
                let targetRange = this.entireRows ? data.getRowsBelow(lines.length - this.rowCount).getEntireRow() : data.getRowsBelow(lines.length - this.rowCount);
                targetRange.insert(Excel.InsertShiftDirection.down);
            }
            else if (lines.length < this.rowCount) {
                let targetRange = this.entireRows ? data.getResizedRange(-lines.length, 0).getEntireRow() : data.getResizedRange(-lines.length, 0);
                targetRange.delete(Excel.DeleteShiftDirection.up);
            }
            this.getDataRange(ctx).values = lines;
            this.rowCount = lines.length;
        }

        public initializeBinding() {
            let self = this;
            Office.context.document.bindings.addFromNamedItemAsync(
                this.tableName,
                Office.BindingType.Table,
                { id: this.tableName },
                function (results) {
                    self.binding = results.value;
                    //this.addBindingsHandler();
                });
        }

        // Event handler to refresh the table when the
        // data in the table changes.
        private onBindingDataChanged = function (result) {
        }

        // Add the handler to the BindingDataChanged event of the binding.
        // When the event is raised, the callback function is called.
        public addBindingsHandler(callback) {
            this.binding.addHandlerAsync(
                Office.EventType.BindingDataChanged,
                this.onBindingDataChanged,
                function (results) { (results.status == Office.AsyncResultStatus.Succeeded && callback && callback()) }
            );
        }

        public removeBindingsHandler(callback) {
            this.binding.removeHandlerAsync(
                Office.EventType.BindingDataChanged,
                { handler: this.onBindingDataChanged },
                function (results) { (results.status == Office.AsyncResultStatus.Succeeded && callback && callback()) }
            );
        }

        toString(): string {
            return this.tableName;
        }

    }

    export class TableResources extends Table {

        constructor() {
            super("tblResourcesData", ["Chk", "RESOURCE_NAME", "SORT_GROUP_ID", "RESOURCE_TYPE_ID", "USER_ID", "COMP_CODE", "SLID", "THIRD_PARTY_NAME", "EXPERIENCE_LEVEL", "COST_CURRENCY_TC", "COST_RATE_TC", "COST_RATE_PC", "INFLATION_RATE"]);
        }

    }

}
