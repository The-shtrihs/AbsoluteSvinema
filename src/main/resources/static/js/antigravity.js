/**
 * AbsoluteSvinema — Antigravity Interactive Particle Canvas
 * Features:
 * - Dynamic constellation particle field
 * - Cursor gravitation & repulsion with spring damping
 * - Interactive hue shifting based on cursor movement
 * - Proximity-based dynamic glowing connection lines
 * - Click shockwave pulse
 * - Ambient spotlight glow
 */
(function () {
    'use strict';

    const canvas = document.getElementById('antigravity-canvas');
    if (!canvas) return;

    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    let width = 0;
    let height = 0;
    let dpr = window.devicePixelRatio || 1;

    // Mouse state
    const mouse = {
        x: -1000,
        y: -1000,
        targetX: -1000,
        targetY: -1000,
        radius: 160,
        isHovered: false,
        hueShift: 185 // Base cyan hue
    };

    // Shockwaves on click
    const shockwaves = [];

    // Particle settings
    const PARTICLE_COUNT = Math.min(100, Math.floor((window.innerWidth * window.innerHeight) / 12000));
    const CONNECT_DISTANCE = 120;
    const particles = [];

    class Particle {
        constructor() {
            this.reset(true);
        }

        reset(initial) {
            this.x = initial ? Math.random() * width : (Math.random() > 0.5 ? 0 : width);
            this.y = initial ? Math.random() * height : Math.random() * height;
            this.baseRadius = Math.random() * 2 + 1.2;
            this.radius = this.baseRadius;
            
            const speed = Math.random() * 0.6 + 0.2;
            const angle = Math.random() * Math.PI * 2;
            this.vx = Math.cos(angle) * speed;
            this.vy = Math.sin(angle) * speed;

            this.baseHue = Math.random() > 0.6 ? 185 : (Math.random() > 0.3 ? 280 : 330); // Cyan, Purple, Pink
            this.hue = this.baseHue;
            this.alpha = Math.random() * 0.5 + 0.3;
            this.pulseSpeed = Math.random() * 0.03 + 0.01;
            this.pulsePhase = Math.random() * Math.PI * 2;
        }

        update() {
            // Natural pulse
            this.pulsePhase += this.pulseSpeed;
            this.radius = this.baseRadius + Math.sin(this.pulsePhase) * 0.8;

            // Mouse interaction (Gravitation & Repulsion)
            const dx = mouse.x - this.x;
            const dy = mouse.y - this.y;
            const dist = Math.sqrt(dx * dx + dy * dy);

            if (dist < mouse.radius && mouse.isHovered) {
                const force = (1 - dist / mouse.radius);
                // Gentle repulsion when very close, light swirl when near
                const angle = Math.atan2(dy, dx);
                this.vx -= Math.cos(angle) * force * 0.5;
                this.vy -= Math.sin(angle) * force * 0.5;

                // Color adapts towards cursor hue
                this.hue = (this.hue * 0.95 + mouse.hueShift * 0.05);
                this.alpha = Math.min(1, this.alpha + 0.05);
            } else {
                // Return slowly to base properties
                this.hue = (this.hue * 0.98 + this.baseHue * 0.02);
                this.alpha = (this.alpha * 0.98 + (0.4) * 0.02);
            }

            // Shockwave influence
            for (let i = 0; i < shockwaves.length; i++) {
                const sw = shockwaves[i];
                const sdx = this.x - sw.x;
                const sdy = this.y - sw.y;
                const sdist = Math.sqrt(sdx * sdx + sdy * sdy);
                const diff = Math.abs(sdist - sw.currentRadius);
                if (diff < sw.thickness) {
                    const waveForce = (1 - diff / sw.thickness) * sw.intensity;
                    const angle = Math.atan2(sdy, sdx);
                    this.vx += Math.cos(angle) * waveForce * 4;
                    this.vy += Math.sin(angle) * waveForce * 4;
                }
            }

            // Speed limit & damping
            const currentSpeed = Math.sqrt(this.vx * this.vx + this.vy * this.vy);
            if (currentSpeed > 3) {
                this.vx *= 0.95;
                this.vy *= 0.95;
            } else {
                this.vx *= 0.99;
                this.vy *= 0.99;
            }

            this.x += this.vx;
            this.y += this.vy;

            // Bounce off boundaries with soft restitution
            if (this.x < 0) {
                this.x = 0;
                this.vx *= -1;
            } else if (this.x > width) {
                this.x = width;
                this.vx *= -1;
            }
            if (this.y < 0) {
                this.y = 0;
                this.vy *= -1;
            } else if (this.y > height) {
                this.y = height;
                this.vy *= -1;
            }
        }

        draw() {
            ctx.beginPath();
            ctx.arc(this.x, this.y, Math.max(0.1, this.radius), 0, Math.PI * 2);
            ctx.fillStyle = `hsla(${this.hue}, 90%, 65%, ${this.alpha})`;
            ctx.shadowBlur = 8;
            ctx.shadowColor = `hsla(${this.hue}, 100%, 50%, 0.6)`;
            ctx.fill();
            ctx.shadowBlur = 0;
        }
    }

    function resize() {
        width = window.innerWidth;
        height = window.innerHeight;
        dpr = window.devicePixelRatio || 1;
        canvas.width = width * dpr;
        canvas.height = height * dpr;
        ctx.scale(dpr, dpr);

        if (particles.length === 0) {
            for (let i = 0; i < PARTICLE_COUNT; i++) {
                particles.push(new Particle());
            }
        }
    }

    // Connect close particles with glowing gradient lines
    function drawConnections() {
        for (let i = 0; i < particles.length; i++) {
            for (let j = i + 1; j < particles.length; j++) {
                const p1 = particles[i];
                const p2 = particles[j];
                const dx = p1.x - p2.x;
                const dy = p1.y - p2.y;
                const dist = Math.sqrt(dx * dx + dy * dy);

                if (dist < CONNECT_DISTANCE) {
                    const alpha = (1 - dist / CONNECT_DISTANCE) * 0.25;
                    const gradient = ctx.createLinearGradient(p1.x, p1.y, p2.x, p2.y);
                    gradient.addColorStop(0, `hsla(${p1.hue}, 85%, 60%, ${alpha})`);
                    gradient.addColorStop(1, `hsla(${p2.hue}, 85%, 60%, ${alpha})`);

                    ctx.beginPath();
                    ctx.moveTo(p1.x, p1.y);
                    ctx.lineTo(p2.x, p2.y);
                    ctx.strokeStyle = gradient;
                    ctx.lineWidth = 1;
                    ctx.stroke();
                }
            }
        }
    }

    // Update and draw shockwaves
    function drawShockwaves() {
        for (let i = shockwaves.length - 1; i >= 0; i--) {
            const sw = shockwaves[i];
            sw.currentRadius += sw.speed;
            sw.intensity *= 0.94;

            if (sw.intensity < 0.02 || sw.currentRadius > sw.maxRadius) {
                shockwaves.splice(i, 1);
                continue;
            }

            ctx.beginPath();
            ctx.arc(sw.x, sw.y, sw.currentRadius, 0, Math.PI * 2);
            ctx.strokeStyle = `hsla(${sw.hue}, 100%, 70%, ${sw.intensity * 0.5})`;
            ctx.lineWidth = 2;
            ctx.stroke();
        }
    }

    // Animation Loop
    function animate() {
        // Smooth mouse follow
        mouse.x += (mouse.targetX - mouse.x) * 0.15;
        mouse.y += (mouse.targetY - mouse.y) * 0.15;

        // Clear canvas
        ctx.clearRect(0, 0, width, height);

        // Draw ambient glow at mouse
        if (mouse.isHovered) {
            const radialGlow = ctx.createRadialGradient(
                mouse.x, mouse.y, 0,
                mouse.x, mouse.y, mouse.radius * 1.5
            );
            radialGlow.addColorStop(0, `hsla(${mouse.hueShift}, 100%, 65%, 0.1)`);
            radialGlow.addColorStop(0.5, `hsla(${mouse.hueShift + 40}, 90%, 55%, 0.04)`);
            radialGlow.addColorStop(1, 'transparent');

            ctx.beginPath();
            ctx.arc(mouse.x, mouse.y, mouse.radius * 1.5, 0, Math.PI * 2);
            ctx.fillStyle = radialGlow;
            ctx.fill();
        }

        // Draw connections
        drawConnections();

        // Draw shockwaves
        drawShockwaves();

        // Update & draw particles
        for (let i = 0; i < particles.length; i++) {
            particles[i].update();
            particles[i].draw();
        }

        requestAnimationFrame(animate);
    }

    // Event listeners
    window.addEventListener('resize', resize);

    window.addEventListener('mousemove', (e) => {
        mouse.targetX = e.clientX;
        mouse.targetY = e.clientY;
        mouse.isHovered = true;

        // Dynamic color shifting based on horizontal & vertical position
        const normalizedX = e.clientX / window.innerWidth;
        const normalizedY = e.clientY / window.innerHeight;
        mouse.hueShift = (180 + normalizedX * 120 + normalizedY * 60) % 360;
    });

    window.addEventListener('mouseleave', () => {
        mouse.isHovered = false;
        mouse.targetX = -1000;
        mouse.targetY = -1000;
    });

    window.addEventListener('click', (e) => {
        shockwaves.push({
            x: e.clientX,
            y: e.clientY,
            currentRadius: 10,
            maxRadius: 280,
            speed: 6,
            thickness: 35,
            intensity: 1,
            hue: mouse.hueShift
        });
    });

    // Initialize
    resize();
    requestAnimationFrame(animate);
})();
