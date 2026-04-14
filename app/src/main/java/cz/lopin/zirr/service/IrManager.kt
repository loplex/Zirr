package cz.lopin.zirr.service

import android.content.Context
import android.hardware.ConsumerIrManager
import cz.lopin.zirr.data.model.IrCommand

class IrManager(context: Context) {
    private val irManager: ConsumerIrManager? =
        context.getSystemService(Context.CONSUMER_IR_SERVICE) as? ConsumerIrManager

    fun hasIrEmitter(): Boolean {
        return irManager?.hasIrEmitter() ?: false
    }

    fun transmit(command: IrCommand) {
        if (hasIrEmitter()) {
            irManager?.transmit(command.frequency, command.pattern.toIntArray())
        }
    }
}
