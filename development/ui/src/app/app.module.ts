import {LayoutModule} from "@angular/cdk/layout";
import {HTTP_INTERCEPTORS, HttpClientModule} from "@angular/common/http";
import {NgModule} from "@angular/core";
import {MatButtonModule, MatIconModule, MatIconRegistry, MatSidenavModule} from "@angular/material";
import {BrowserModule} from "@angular/platform-browser";
import {BrowserAnimationsModule} from "@angular/platform-browser/animations";
import {CookieService} from "ngx-cookie-service";
import {MarkdownModule} from "ngx-markdown";
import {AppRoutingModule} from "./app-routing.module";
import {AppComponent} from "./app.component";
import {AppService} from "./app.service";
import {PageService} from "./components/shared/page.service";
import {SharedModule} from "./components/shared/shared.module";
import {IconService} from "./icon.service";
import {PoiService} from "./poi.service";
import {SpinnerInterceptor} from "./spinner/spinner-interceptor";
import {SpinnerModule} from "./spinner/spinner.module";
import {SpinnerService} from "./spinner/spinner.service";
import {UserService} from "./user.service";
import { ServiceWorkerModule } from '@angular/service-worker';
import { environment } from '../environments/environment';

@NgModule({
  declarations: [
    AppComponent
  ],
  imports: [
    MarkdownModule.forRoot(),
    BrowserModule,
    BrowserAnimationsModule,
    LayoutModule,
    MatIconModule,
    MatSidenavModule,
    MatButtonModule,
    HttpClientModule,
    SharedModule,
    AppRoutingModule,
    SpinnerModule,
    ServiceWorkerModule.register('ngsw-worker.js', { enabled: environment.production })
  ],
  providers: [
    {provide: HTTP_INTERCEPTORS, useClass: SpinnerInterceptor, multi: true},
    CookieService,
    UserService,
    AppService,
    SpinnerService,
    PageService,
    MatIconRegistry,
    IconService,
    PoiService
  ],
  bootstrap: [
    AppComponent
  ]
})
export class AppModule {
}
