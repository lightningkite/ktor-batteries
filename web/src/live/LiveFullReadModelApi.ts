// Package: com.lightningkite.ktordb.live
// Generated by Khrysalis - this file will be overwritten.
import { FullReadModelApi } from '../FullReadModelApi'
import { HasId } from '../HasId'
import { ObserveModelApi } from '../ObserveModelApi'
import { LiveObserveModelApi } from './LiveObserveModelApi'
import { LiveReadModelApi } from './LiveReadModelApi'

//! Declares com.lightningkite.ktordb.live.LiveFullReadModelApi
export class LiveFullReadModelApi<Model extends HasId> extends FullReadModelApi<Model> {
    public constructor(public readonly read: LiveReadModelApi<Model>, public readonly observe: ObserveModelApi<Model>) {
        super();
    }
    
    
}
export namespace LiveFullReadModelApi {
    //! Declares com.lightningkite.ktordb.live.LiveFullReadModelApi.Companion
    export class Companion {
        private constructor() {
        }
        public static INSTANCE = new Companion();
        
        public create<Model extends HasId>(Model: Array<any>, root: string, multiplexSocketUrl: string, path: string, token: string): LiveFullReadModelApi<Model> {
            return new LiveFullReadModelApi<Model>(new LiveReadModelApi<Model>(`${root}${path}`, token, Model), LiveObserveModelApi.Companion.INSTANCE.create<Model>(Model, multiplexSocketUrl, token, path));
        }
    }
}