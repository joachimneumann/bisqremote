//
//  BisqNotifications.swift
//  bisqremote
//
//  Created by Joachim Neumann on 04/06/2018.
//  Copyright © 2018 joachim Neumann. All rights reserved.
//

import Foundation

let userDefaultKey = "bisqNotification"

struct ANotification: Codable {
    var bisqNotificationVersion: Int
    var alert: String
    var badge: Int
    var sound: String
    var bisqMessage: String
}

class BisqNotifications {

    static let shared = BisqNotifications()
    private var array: [ANotification] = [ANotification]()
    private let decoder = JSONDecoder()
    private let encoder = JSONEncoder()
    private init() {
        load()
    }

    func parseArray(json: String) {
        do {
            let data: Data? = json.data(using: .utf8)
            array = try decoder.decode([ANotification].self, from: data!)
        } catch {
            array = [ANotification]()
        }
    }

    func parse(json: String) -> ANotification? {
        var ret: ANotification?
        do {
            let data: Data? = json.data(using: .utf8)
            ret = try decoder.decode(ANotification.self, from: data!)
        } catch {
            ret = nil
        }
        return ret
    }

    private func save() {
        do {
            let jsonData = try encoder.encode(array)
            let toDefaults = String(data: jsonData, encoding: .utf8)!
            UserDefaults.standard.set(toDefaults, forKey: userDefaultKey)
        } catch {
            print("/n###/n### save failed/n###/n")
        }
    }
    
    private func load() {
        let fromDefaults = UserDefaults.standard.string(forKey: userDefaultKey) ?? "[]"
        parseArray(json: fromDefaults)
    }
    
    var count: Int {
        return array.count
    }
    
    func at(n: Int) -> ANotification {
        return array[n]
    }
    
    
    func add(new: ANotification) {
        array.append(new)
        save()
    }
    
    func remove(n: Int) {
        array.remove(at: n)
        save()
    }
}

