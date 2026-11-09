import SwiftUI
import UIKit
import VadretShared

@available(iOS 26.0, *)
struct RouteWrapper: Hashable, Identifiable {
    let id = UUID()
    let route: NativeRoute

    static func == (lhs: RouteWrapper, rhs: RouteWrapper) -> Bool {
        lhs.id == rhs.id
    }

    func hash(into hasher: inout Hasher) {
        hasher.combine(id)
    }
}

@available(iOS 26.0, *)
@Observable
final class TabNavigationCoordinator {
    var path: [RouteWrapper] = []

    func push(_ route: NativeRoute) {
        if path.last?.route != route {
            path.append(RouteWrapper(route: route))
        }
    }

    func pop(_ wrapper: RouteWrapper) {
        if path.last == wrapper {
            path.removeLast()
        }
    }

    func popToRoot() {
        path.removeAll()
    }
}

@available(iOS 26.0, *)
@Observable
final class AppNavigationCoordinator {
    @ObservationIgnored lazy var shell = NativeShell()
    let tabs: [NativeTab] = NativeTab.entries
    var selectedTab: NativeTab = .weather
    var presentedRoute: RouteWrapper?
    var appearance: NativeAppearance?
    private let tabCoordinators: [NativeTab: TabNavigationCoordinator] = Dictionary(
        uniqueKeysWithValues: NativeTab.entries.map { tab in (tab, TabNavigationCoordinator()) }
    )

    func start() {
        shell.start(
            onAppearanceChange: { [weak self] appearance in self?.apply(appearance: appearance) },
            onStoryOpen: { [weak self] route in self?.present(route: route) }
        )
    }

    func coordinator(tab: NativeTab) -> TabNavigationCoordinator {
        tabCoordinators[tab]!
    }

    func title(tab: NativeTab) -> String {
        appearance?.title(tab: tab) ?? ""
    }

    func select(tab: NativeTab) {
        if tab == selectedTab {
            coordinator(tab: tab).popToRoot()
        }
        selectedTab = tab
    }

    func open(tab: NativeTab) {
        selectedTab = tab
        coordinator(tab: tab).popToRoot()
    }

    func navigate(
        route: NativeRoute,
        tab: NativeTab
    ) {
        if route.presentation == .sheet || route.presentation == .fullScreenCover {
            present(route: route)
        } else {
            coordinator(tab: tab).push(route)
        }
    }

    func present(route: NativeRoute) {
        presentedRoute = RouteWrapper(route: route)
    }

    func dismiss(_ wrapper: RouteWrapper) {
        if presentedRoute == wrapper {
            presentedRoute = nil
        }
    }

    private func apply(appearance: NativeAppearance) {
        self.appearance = appearance
        for scene in UIApplication.shared.connectedScenes {
            for window in (scene as? UIWindowScene)?.windows ?? [] {
                appearance.applyInterfaceStyle(window: window)
            }
        }
    }
}

@available(iOS 26.0, *)
struct NativeNavigationView: View {
    @State private var appCoordinator = AppNavigationCoordinator()

    var body: some View {
        TabView(
            selection: Binding(
                get: { appCoordinator.selectedTab },
                set: { tab in appCoordinator.select(tab: tab) }
            )
        ) {
            ForEach(appCoordinator.tabs, id: \.self) { tab in
                Tab(appCoordinator.title(tab: tab), systemImage: tab.systemImageName, value: tab) {
                    TabContentView(
                        tab: tab,
                        coordinator: appCoordinator.coordinator(tab: tab),
                        appCoordinator: appCoordinator
                    )
                }
            }
        }
        .tabBarMinimizeBehavior(.automatic)
        .tint(Color(.accent))
        .sheet(item: presentedRoute(presentation: .sheet)) { wrapper in
            SheetScreenView(
                wrapper: wrapper,
                appCoordinator: appCoordinator
            )
        }
        .fullScreenCover(item: presentedRoute(presentation: .fullScreenCover)) { wrapper in
            PresentedComposeView(
                wrapper: wrapper,
                appCoordinator: appCoordinator
            )
            .ignoresSafeArea()
        }
        .task {
            appCoordinator.start()
        }
    }

    private func presentedRoute(presentation: NativePresentation) -> Binding<RouteWrapper?> {
        Binding(
            get: {
                appCoordinator.presentedRoute?.route.presentation == presentation ? appCoordinator.presentedRoute : nil
            },
            set: { wrapper in
                if wrapper == nil, let presentedRoute = appCoordinator.presentedRoute, presentedRoute.route.presentation == presentation {
                    appCoordinator.dismiss(presentedRoute)
                }
            }
        )
    }
}

@available(iOS 26.0, *)
struct TabContentView: View {
    let tab: NativeTab
    @Bindable var coordinator: TabNavigationCoordinator
    let appCoordinator: AppNavigationCoordinator

    var body: some View {
        NavigationStack(path: $coordinator.path) {
            TabRootComposeView(
                tab: tab,
                appCoordinator: appCoordinator
            )
            .ignoresSafeArea()
            .background(TabBarShadow())
            .navigationTitle(appCoordinator.title(tab: tab))
            .toolbarVisibility(.hidden, for: .navigationBar)
            .navigationDestination(for: RouteWrapper.self) { wrapper in
                ScreenComposeView(
                    route: wrapper.route,
                    appCoordinator: appCoordinator,
                    onNavigate: { route in appCoordinator.navigate(route: route, tab: tab) },
                    onGoBack: { coordinator.pop(wrapper) }
                )
                .ignoresSafeArea()
                .navigationTitle(wrapper.route.title)
                .toolbarTitleDisplayMode(.inline)
                .toolbarVisibility(
                    wrapper.route.presentation == .pushWithoutNavigationBar ? .hidden : .automatic,
                    for: .navigationBar
                )
            }
        }
    }
}

@available(iOS 26.0, *)
struct SheetScreenView: View {
    let wrapper: RouteWrapper
    let appCoordinator: AppNavigationCoordinator

    var body: some View {
        NavigationStack {
            PresentedComposeView(
                wrapper: wrapper,
                appCoordinator: appCoordinator
            )
            .ignoresSafeArea()
            .navigationTitle(wrapper.route.title)
            .toolbarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(role: .close) {
                        appCoordinator.dismiss(wrapper)
                    }
                }
            }
        }
    }
}

@available(iOS 26.0, *)
struct PresentedComposeView: View {
    let wrapper: RouteWrapper
    let appCoordinator: AppNavigationCoordinator

    var body: some View {
        ScreenComposeView(
            route: wrapper.route,
            appCoordinator: appCoordinator,
            onNavigate: { route in appCoordinator.navigate(route: route, tab: appCoordinator.selectedTab) },
            onGoBack: { appCoordinator.dismiss(wrapper) }
        )
    }
}

@available(iOS 26.0, *)
struct TabRootComposeView: UIViewControllerRepresentable {
    let tab: NativeTab
    let appCoordinator: AppNavigationCoordinator

    func makeUIViewController(context: Context) -> UIViewController {
        appCoordinator.shell.tabViewController(
            tab: tab,
            onNavigate: { route in appCoordinator.navigate(route: route, tab: tab) },
            onSelectTab: { selectedTab in appCoordinator.open(tab: selectedTab) }
        )
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

@available(iOS 26.0, *)
struct ScreenComposeView: UIViewControllerRepresentable {
    let route: NativeRoute
    let appCoordinator: AppNavigationCoordinator
    let onNavigate: (NativeRoute) -> Void
    let onGoBack: () -> Void

    func makeUIViewController(context: Context) -> UIViewController {
        let screenViewController = appCoordinator.shell.screenViewController(
            route: route,
            onNavigate: onNavigate,
            onGoBack: onGoBack,
            onSelectTab: { selectedTab in appCoordinator.open(tab: selectedTab) }
        )
        if route.presentation == .pushWithoutNavigationBar {
            return SwipeBackViewController(content: screenViewController)
        }
        return screenViewController
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
