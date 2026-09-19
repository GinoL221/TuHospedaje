export default function GuestDetails({
  user,
  guestPhone,
  guestDetailsExpanded,
  phoneError,
  phoneInputRef,
  onGuestPhoneChange,
  onToggle,
}) {
  return (
    <>
      <fieldset className="booking-fieldset booking-identity-fieldset">
        <legend>Tus datos</legend>
        <div className="booking-field-grid booking-field-grid--identity">
          <div className="booking-field">
            <label htmlFor="booking-first-name">Nombre</label>
            <input
              id="booking-first-name"
              value={user.firstName}
              readOnly
              autoComplete="given-name"
            />
          </div>

          <div className="booking-field">
            <label htmlFor="booking-last-name">Apellido</label>
            <input
              id="booking-last-name"
              value={user.lastName}
              readOnly
              autoComplete="family-name"
            />
          </div>

          <div className="booking-field booking-field--full">
            <label htmlFor="booking-email">Email</label>
            <input
              id="booking-email"
              type="email"
              value={user.email}
              readOnly
              autoComplete="email"
            />
          </div>
        </div>
      </fieldset>

      <fieldset className="booking-fieldset booking-guest-fieldset">
        <legend>Detalles del huésped</legend>
        <button
          type="button"
          className="guest-details-toggle"
          aria-expanded={guestDetailsExpanded}
          aria-controls="guest-details"
          aria-describedby={phoneError ? "booking-phone-error" : undefined}
          onClick={onToggle}
        >
          {guestDetailsExpanded
            ? "Ocultar detalles del huésped"
            : "Mostrar detalles del huésped"}
        </button>

        {guestDetailsExpanded && (
          <section id="guest-details" aria-label="Detalles del huésped">
            {user.imageUrl && (
              <img
                src={user.imageUrl}
                alt={`Perfil de ${user.firstName} ${user.lastName}`}
                className="guest-profile-image"
              />
            )}
            <div className="booking-phone-field">
              <label htmlFor="booking-phone">Teléfono</label>
              <input
                id="booking-phone"
                ref={phoneInputRef}
                type="tel"
                inputMode="tel"
                autoComplete="tel"
                value={guestPhone}
                onChange={onGuestPhoneChange}
                placeholder="Ingresá tu teléfono"
                aria-required="true"
                aria-invalid={phoneError ? "true" : undefined}
                aria-describedby={
                  phoneError ? "booking-phone-error" : undefined
                }
              />
              {phoneError && (
                <p
                  id="booking-phone-error"
                  className="error"
                  role="alert"
                  aria-live="assertive"
                >
                  {phoneError}
                </p>
              )}
            </div>
          </section>
        )}
      </fieldset>
    </>
  );
}
