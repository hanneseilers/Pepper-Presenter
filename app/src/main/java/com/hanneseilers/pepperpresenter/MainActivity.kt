package com.hanneseilers.pepperpresenter

import android.os.Bundle
import com.aldebaran.qi.sdk.QiSDK
import com.aldebaran.qi.sdk.RobotLifecycleCallbacks
import com.aldebaran.qi.sdk.builder.SayBuilder
import com.aldebaran.qi.sdk.`object`.conversation.Phrase
import com.aldebaran.qi.sdk.`object`.conversation.Say
import com.aldebaran.qi.sdk.design.activity.RobotActivity
import com.aldebaran.qi.sdk.robot.RobotContext

class MainActivity : RobotActivity(), RobotLifecycleCallbacks {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        QiSDK.register(this, this)
    }

    override fun onDestroy() {
        QiSDK.unregister(this, this)
        super.onDestroy()
    }

    override fun onRobotFocusGained(robotContext: RobotContext) {
        SayBuilder.with(robotContext)
            .withPhrase(Phrase("Pepper Presenter is ready."))
            .buildAsync()
            .andThenCompose { say: Say -> say.async().run() }
    }

    override fun onRobotFocusLost() = Unit

    override fun onRobotFocusRefused(reason: String?) = Unit
}
