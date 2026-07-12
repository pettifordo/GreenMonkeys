// Green Monkeys app icon generator — a guilty-looking green monkey with a
// slipping halo on a night-out purple gradient. Run: swift draw_icon.swift out.png
import AppKit

let out = CommandLine.arguments.count > 1 ? CommandLine.arguments[1] : "icon.png"
let px = 1024

let rep = NSBitmapImageRep(
    bitmapDataPlanes: nil, pixelsWide: px, pixelsHigh: px,
    bitsPerSample: 8, samplesPerPixel: 4, hasAlpha: true, isPlanar: false,
    colorSpaceName: .deviceRGB, bytesPerRow: 0, bitsPerPixel: 0
)!
let gctx = NSGraphicsContext(bitmapImageRep: rep)!
NSGraphicsContext.current = gctx
let ctx = gctx.cgContext

// Flip to top-left origin so the design coordinates read naturally.
ctx.translateBy(x: 0, y: CGFloat(px))
ctx.scaleBy(x: 1, y: -1)

func rgb(_ hex: UInt32) -> CGColor {
    CGColor(red: CGFloat((hex >> 16) & 0xFF) / 255,
            green: CGFloat((hex >> 8) & 0xFF) / 255,
            blue: CGFloat(hex & 0xFF) / 255, alpha: 1)
}

// MARK: Background — deep night-out purple gradient
let colors = [rgb(0x3B2360), rgb(0x1A1033)] as CFArray
let gradient = CGGradient(colorsSpace: CGColorSpaceCreateDeviceRGB(), colors: colors, locations: [0, 1])!
ctx.drawLinearGradient(gradient, start: CGPoint(x: 512, y: 0), end: CGPoint(x: 512, y: 1024), options: [])

// Soft spotlight glow behind the monkey
ctx.setFillColor(CGColor(red: 1, green: 1, blue: 1, alpha: 0.07))
ctx.fillEllipse(in: CGRect(x: 512 - 400, y: 460 - 400, width: 800, height: 800))

let green = rgb(0x3E9B4F)
let lightGreen = rgb(0xA9E2A0)
let dark = rgb(0x1E4D26)

// MARK: Ears (behind head)
for cx: CGFloat in [252, 772] {
    ctx.setFillColor(green)
    ctx.fillEllipse(in: CGRect(x: cx - 112, y: 470 - 112, width: 224, height: 224))
    ctx.setFillColor(lightGreen)
    ctx.fillEllipse(in: CGRect(x: cx - 58, y: 470 - 58, width: 116, height: 116))
}

// MARK: Head
ctx.setFillColor(green)
ctx.fillEllipse(in: CGRect(x: 512 - 300, y: 520 - 285, width: 600, height: 570))

// MARK: Face marking (light patches: two eye circles + muzzle)
ctx.setFillColor(lightGreen)
ctx.fillEllipse(in: CGRect(x: 415 - 108, y: 470 - 108, width: 216, height: 216))
ctx.fillEllipse(in: CGRect(x: 609 - 108, y: 470 - 108, width: 216, height: 216))
ctx.fillEllipse(in: CGRect(x: 512 - 215, y: 615 - 165, width: 430, height: 330))

// MARK: Left eye — open, guilty sideways glance
ctx.setFillColor(.white)
ctx.fillEllipse(in: CGRect(x: 415 - 62, y: 478 - 68, width: 124, height: 136))
ctx.setFillColor(rgb(0x14100C))
ctx.fillEllipse(in: CGRect(x: 434 - 29, y: 497 - 29, width: 58, height: 58))
ctx.setFillColor(.white)
ctx.fillEllipse(in: CGRect(x: 423 - 10, y: 484 - 10, width: 20, height: 20))

// MARK: Right eye — cheeky wink
ctx.setStrokeColor(rgb(0x14100C))
ctx.setLineWidth(18)
ctx.setLineCap(.round)
ctx.beginPath()
ctx.move(to: CGPoint(x: 552, y: 470))
ctx.addQuadCurve(to: CGPoint(x: 666, y: 470), control: CGPoint(x: 609, y: 524))
ctx.strokePath()

// MARK: Nostrils
ctx.setFillColor(dark)
ctx.fillEllipse(in: CGRect(x: 478 - 13, y: 606 - 19, width: 26, height: 38))
ctx.fillEllipse(in: CGRect(x: 546 - 13, y: 606 - 19, width: 26, height: 38))

// MARK: Lopsided "who, me?" grin
ctx.setStrokeColor(dark)
ctx.setLineWidth(18)
ctx.beginPath()
ctx.move(to: CGPoint(x: 398, y: 690))
ctx.addQuadCurve(to: CGPoint(x: 644, y: 672), control: CGPoint(x: 522, y: 768))
ctx.strokePath()
// smirk tick at the right corner
ctx.beginPath()
ctx.move(to: CGPoint(x: 644, y: 672))
ctx.addQuadCurve(to: CGPoint(x: 668, y: 640), control: CGPoint(x: 666, y: 664))
ctx.strokePath()

// MARK: The slipping halo — innocence, unconvincingly worn
ctx.saveGState()
ctx.translateBy(x: 648, y: 178)
ctx.rotate(by: -0.20)
ctx.setStrokeColor(rgb(0xFFD54F))
ctx.setLineWidth(26)
ctx.strokeEllipse(in: CGRect(x: -140, y: -42, width: 280, height: 84))
ctx.restoreGState()

NSGraphicsContext.current = nil
let png = rep.representation(using: .png, properties: [:])!
try! png.write(to: URL(fileURLWithPath: out))
print("wrote \(out)")
