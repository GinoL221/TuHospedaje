import { Component } from "react";

export default class RouteChunkErrorBoundary extends Component {
  state = { hasError: false };

  static getDerivedStateFromError() {
    return { hasError: true };
  }

  componentDidUpdate(previousProps) {
    if (this.state.hasError && previousProps.resetKey !== this.props.resetKey) {
      this.setState({ hasError: false });
    }
  }

  handleReload = () => {
    window.location.reload();
  };

  render() {
    if (this.state.hasError) {
      return (
        <div className="route-error" role="alert">
          <p>No pudimos cargar esta página.</p>
          <button type="button" onClick={this.handleReload}>
            Recargar página
          </button>
        </div>
      );
    }

    return this.props.children;
  }
}
