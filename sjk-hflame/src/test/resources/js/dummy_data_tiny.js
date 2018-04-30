var data = {
    frames: [
        "(WAITING)",
        "(BLOCKED)",
        "(RUNNING)",
        "(SLEEP)",
        "(IO)",
        "Thread.run", // 5
        "Object.wait",
        "MyClass.a",
        "MyClass.b",
        "MyClass.c",
        "MyMain.run"
    ],
    frameColors: [
        null,
        null,
        null,
        null,
        null,
        "#f65", // 5
        "#f56",
        "#f74",
        "#f47",
        "#e66",
        "#e57"                    
    ],
    threads: [
        {
            name: "Main",
            traces: [
                { trace: [5, 6, 0], samples: 100 },
                { trace: [5, 7], samples: 30 },
                { trace: [5, 7, 8, 2], samples: 15},
                { trace: [5, 7, 9, 2], samples: 15},
                { trace: [10, 7, 9, 2], samples: 25},
            ]
        }
    ]
}            
