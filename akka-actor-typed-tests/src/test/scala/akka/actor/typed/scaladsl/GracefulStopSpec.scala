/**
 * Copyright (C) 2017 Lightbend Inc. <http://www.lightbend.com>
 */
package akka.actor.typed
package scaladsl

import akka.Done
import akka.NotUsed
import akka.testkit.typed.TestKit
import akka.testkit.typed.scaladsl.TestProbe

final class GracefulStopSpec extends TestKit with TypedAkkaSpecWithShutdown {

  "Graceful stop" must {

    "properly stop the children and perform the cleanup" in {
      val probe = TestProbe[Done]("probe")
      val probeCld1 = TestProbe[Done]("probeCld1")
      val probeCld2 = TestProbe[Done]("probeCld2")

      val behavior =
        Behaviors.deferred[akka.NotUsed] { context ⇒
          val c1 = context.spawn[NotUsed](Behaviors.onSignal {
            case (_, PostStop) ⇒
              probeCld1.ref ! Done
              Behaviors.stopped
          }, "child1")

          val c2 = context.spawn[NotUsed](Behaviors.onSignal {
            case (_, PostStop) ⇒
              probeCld2.ref ! Done
              Behaviors.stopped
          }, "child2")

          Behaviors.stopped {
            Behaviors.onSignal {
              case (ctx, PostStop) ⇒
                // cleanup function body
                probe.ref ! Done
                Behaviors.same
            }
          }
        }

      spawn(behavior)
      probeCld1.expectMsg(Done)
      probeCld2.expectMsg(Done)
      probe.expectMsg(Done)
    }

    "properly perform the cleanup and stop itself for no children case" in {
      val probe = TestProbe[Done]("probe")

      val behavior =
        Behaviors.deferred[akka.NotUsed] { context ⇒
          // do not spawn any children
          Behaviors.stopped {
            Behaviors.onSignal {
              case (ctx, PostStop) ⇒
                // cleanup function body
                probe.ref ! Done
                Behaviors.same
            }
          }
        }

      spawn(behavior)
      probe.expectMsg(Done)
    }
  }

}
