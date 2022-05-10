// Package: com.lightningkite.ktordb.live
// Generated by Khrysalis - this file will be overwritten.
import { CompleteModelApi } from '../CompleteModelApi'
import { HasId } from '../HasId'
import { ObserveModelApi } from '../ObserveModelApi'
import { ReadModelApi } from '../ReadModelApi'
import { WriteModelApi } from '../WriteModelApi'
import { LiveObserveModelApi } from './LiveObserveModelApi'
import { LiveReadModelApi } from './LiveReadModelApi'
import { LiveWriteModelApi } from './LiveWriteModelApi'

//! Declares com.lightningkite.ktordb.live.LiveCompleteModelApi
export class LiveCompleteModelApi<Model extends HasId> extends CompleteModelApi<Model> {
    public constructor(public readonly read: ReadModelApi<Model>, public readonly write: WriteModelApi<Model>, public readonly observe: ObserveModelApi<Model>) {
        super();
    }
    
    
}
export namespace LiveCompleteModelApi {
    //! Declares com.lightningkite.ktordb.live.LiveCompleteModelApi.Companion
    export class Companion {
        private constructor() {
        }
        public static INSTANCE = new Companion();
        
        public create<Model extends HasId>(Model: Array<any>, root: string, multiplexSocketUrl: string, path: string, token: string): LiveCompleteModelApi<Model> {
            return new LiveCompleteModelApi<Model>(new LiveReadModelApi<Model>(`${root}${path}`, token, Model), new LiveWriteModelApi<Model>(`${root}${path}`, token, Model), LiveObserveModelApi.Companion.INSTANCE.create<Model>(Model, multiplexSocketUrl, token, path));
        }
    }
}