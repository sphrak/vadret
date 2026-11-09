import SwiftUI
import UIKit

@available(iOS 26.0, *)
struct TabBarShadow: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> TabBarShadowViewController {
        TabBarShadowViewController()
    }

    func updateUIViewController(_ uiViewController: TabBarShadowViewController, context: Context) {}
}

@available(iOS 26.0, *)
final class TabBarShadowViewController: UIViewController {
    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        guard let tabBar = tabBarController?.tabBar else {
            return
        }
        TabBarShadowView.install(in: tabBar)
    }
}

@available(iOS 26.0, *)
final class TabBarShadowView: UIView {
    private static let shadowOpacity: Float = 0.28
    private static let shadowRadius: CGFloat = 16
    private static let shadowDrop: CGFloat = 6
    private static let shadowRoom: CGFloat = 48

    private let outsideMask = CAShapeLayer()

    static func install(in tabBar: UITabBar) {
        let isInstalled: Bool = tabBar.subviews.contains { subview in subview is TabBarShadowView }
        let pill: UIView? = tabBar.subviews.max { first, second in
            first.bounds.width * first.bounds.height < second.bounds.width * second.bounds.height
        }
        guard !isInstalled, let pill else {
            return
        }
        let shadowView = TabBarShadowView()
        tabBar.insertSubview(shadowView, belowSubview: pill)
        NSLayoutConstraint.activate([
            shadowView.leadingAnchor.constraint(equalTo: pill.leadingAnchor),
            shadowView.trailingAnchor.constraint(equalTo: pill.trailingAnchor),
            shadowView.topAnchor.constraint(equalTo: pill.topAnchor),
            shadowView.bottomAnchor.constraint(equalTo: pill.bottomAnchor)
        ])
    }

    override init(frame: CGRect) {
        super.init(frame: frame)
        translatesAutoresizingMaskIntoConstraints = false
        isUserInteractionEnabled = false
        backgroundColor = .clear
        layer.shadowColor = UIColor.black.cgColor
        layer.shadowOpacity = Self.shadowOpacity
        layer.shadowRadius = Self.shadowRadius
        layer.shadowOffset = CGSize(width: 0, height: Self.shadowDrop)
        outsideMask.fillRule = .evenOdd
        layer.mask = outsideMask
    }

    required init?(coder: NSCoder) {
        nil
    }

    override func layoutSubviews() {
        super.layoutSubviews()
        let pillPath = UIBezierPath(roundedRect: bounds, cornerRadius: bounds.height / 2)
        layer.shadowPath = pillPath.cgPath
        let outsidePath = UIBezierPath(rect: bounds.insetBy(dx: -Self.shadowRoom, dy: -Self.shadowRoom))
        outsidePath.append(pillPath)
        outsideMask.path = outsidePath.cgPath
    }
}
