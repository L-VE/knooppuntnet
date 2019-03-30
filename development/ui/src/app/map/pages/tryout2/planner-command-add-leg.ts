import {List} from "immutable";
import {PlannerCommand} from "./planner-command";
import {PlannerContext} from "./planner-context";
import {Plan} from "./plan";
import {PlanNode} from "./plan-node";
import {PlanLeg} from "./plan-leg";
import {PlanLegFragment} from "./plan-leg-fragment";

export class PlannerCommandAddLeg implements PlannerCommand {

  constructor(private legId: string,
              private source: PlanNode,
              private sink: PlanNode) {
  }

  public do(context: PlannerContext) {
    context.routeLayer.addViaNodeFlag(this.legId, this.sink.nodeId, this.sink.coordinate);
    let fragments: List<PlanLegFragment> = List();
    const cachedLeg = context.legCache.get(this.source.nodeId, this.sink.nodeId);
    if (cachedLeg) {
      fragments = cachedLeg.fragments;
      const coordinates = fragments.flatMap(f => f.coordinates);
      context.routeLayer.addRouteLeg(this.legId, coordinates);
    }
    const leg = new PlanLeg(this.legId, this.source, this.sink, fragments);
    const newLegs = context.plan.legs.push(leg);
    const newPlan = new Plan(context.plan.source, newLegs);
    context.updatePlan(newPlan);
  }

  public undo(context: PlannerContext) {
    const legs = context.plan.legs;
    const newLegs = legs.setSize(legs.size - 1);
    const plan = new Plan(context.plan.source, newLegs);
    context.updatePlan(plan);
    context.routeLayer.removeRouteLeg(this.legId);
    context.routeLayer.removeViaNodeFlag(this.legId, this.sink.nodeId);
  }

}
