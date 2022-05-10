"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.MockFullReadModelApi = void 0;
const FullReadModelApi_1 = require("../FullReadModelApi");
const MockObserveModelApi_1 = require("./MockObserveModelApi");
const MockReadModelApi_1 = require("./MockReadModelApi");
//! Declares com.lightningkite.ktordb.mock.MockFullReadModelApi
class MockFullReadModelApi extends FullReadModelApi_1.FullReadModelApi {
    constructor(table) {
        super();
        this.table = table;
        this.read = new MockReadModelApi_1.MockReadModelApi(this.table);
        this.observe = new MockObserveModelApi_1.MockObserveModelApi(this.table);
    }
}
exports.MockFullReadModelApi = MockFullReadModelApi;
//# sourceMappingURL=MockFullReadModelApi.js.map