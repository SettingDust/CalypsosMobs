package settingdust.calypsos_mobs.adapter

import settingdust.calypsos_mobs.ServiceLoaderUtil

interface Entrypoint {
    companion object : Entrypoint {
        private val services by lazy { ServiceLoaderUtil.findServices<Entrypoint>(required = false) }

        override fun init() {
            services.forEach { it.init() }
        }

        override fun clientInit() {
            services.forEach { it.clientInit() }
        }
    }

    fun init() {}

    fun clientInit() {}
}