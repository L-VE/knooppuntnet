import {PlannerContext} from "./planner-context";
import {BehaviorSubject, Observable} from "rxjs";
import {PlannerMode} from "./planner-mode";
import {PlannerRouteLayer} from "./planner-route-layer";
import {PlannerCrosshairLayer} from "./planner-crosshair-layer";
import {Plan} from "./plan";
import {PlannerCommandStack} from "./planner-command-stack";
import {List} from "immutable";
import {PlanLegFragment} from "./plan-leg-fragment";
import {PlanLeg} from "./plan-leg";
import {PlanLegCache} from "./plan-leg-cache";

export class PlannerContextImpl implements PlannerContext {

  private _mode = new BehaviorSubject<PlannerMode>(PlannerMode.Idle);
  private _plan = new BehaviorSubject<Plan>(new Plan(null, List()));
  public readonly legCache: PlanLegCache = new PlanLegCache();

  constructor(public commandStack: PlannerCommandStack,
              public routeLayer: PlannerRouteLayer,
              public crosshairLayer: PlannerCrosshairLayer) {
  }

  get mode(): Observable<PlannerMode> {
    return this._mode;
  }

  get planObserver(): Observable<Plan> {
    return this._plan;
  }

  get plan(): Plan {
    return this._plan.value;
  }

  updatePlan(plan: Plan) {
    this._plan.next(plan);
  }

  updatePlanLeg(legId: string, fragments: List<PlanLegFragment>) {
    const newLegs = this.plan.legs.map(leg => {
      if (leg.legId === legId) {
        return new PlanLeg(legId, leg.source, leg.sink, fragments);
      }
      return leg;
    });
    const newPlan = new Plan(this.plan.source, newLegs);
    this.updatePlan(newPlan);

    const coordinates = fragments.flatMap(f => f.coordinates); // TODO this duplicates code from PlannerCommandAddLeg.do() - can share?
    this.routeLayer.addRouteLeg(legId, coordinates);
  }

}
